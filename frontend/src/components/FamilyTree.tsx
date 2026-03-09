import { useEffect, useState, useCallback, useRef, useMemo } from 'react';
import { Link } from 'react-router-dom';
import Tree from 'react-d3-tree';
import type { RawNodeDatum, CustomNodeElementProps } from 'react-d3-tree';
import { fetchFamilyTree } from '../api';
import type { FamilyTreeNode } from '../types';
import { getBranchColor } from '../branchColors';
import { Skeleton } from './Skeleton';
import './FamilyTree.css';

function toTreeData(node: FamilyTreeNode): RawNodeDatum {
  return {
    name: node.name,
    attributes: {
      id: node.id,
      generation: node.generation ?? 0,
      ageGroup: node.ageGroup,
    },
    children: node.children.filter(c => c.ageGroup !== 'SPOUSE').map(toTreeData),
  };
}

function displayName(name: string): string {
  return name.split(' - ')[0];
}

function branchKey(fullName: string): string {
  const cleaned = fullName.split(' - ')[0];
  if (cleaned.includes(' II')) return 'Wesley II';
  return cleaned.split(' ')[0];
}

function generationColor(gen: number | string): string {
  const g = typeof gen === 'string' ? parseInt(gen, 10) : gen;
  if (g === 0) return '#c9a84c';
  if (g === 1) return '#2c3e6b';
  if (g === 2) return '#c0392b';
  if (g === 3) return '#1a8a6e';
  return '#8e44ad';
}

function countNodes(node: FamilyTreeNode): number {
  let c = 1;
  for (const child of node.children) c += countNodes(child);
  return c;
}

export default function FamilyTree() {
  const [rawData, setRawData] = useState<{ roots: FamilyTreeNode[]; totalMembers: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [translate, setTranslate] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(0.55);
  const [selectedBranch, setSelectedBranch] = useState<string>('overview');
  const [treeKey, setTreeKey] = useState(0);

  useEffect(() => {
    fetchFamilyTree()
      .then(setRawData)
      .catch((err) => setError(err.message));
  }, []);

  // Extract Gen 1 branches from raw data
  const branches = useMemo(() => {
    if (!rawData) return [];
    const result: FamilyTreeNode[] = [];
    function find(node: FamilyTreeNode) {
      if (node.generation === 1) result.push(node);
      else node.children.forEach(find);
    }
    rawData.roots.forEach(find);
    return result;
  }, [rawData]);

  // Build tree data based on selected branch
  const treeData = useMemo<RawNodeDatum | null>(() => {
    if (!rawData) return null;

    if (selectedBranch === 'overview') {
      // Single Founders node with all Gen 1 children directly underneath
      const gen1Children: FamilyTreeNode[] = [];
      for (const root of rawData.roots) {
        for (const child of root.children) {
          if (child.generation === 1) gen1Children.push(child);
        }
      }
      return {
        name: 'Wesley & Esther Tumblin',
        attributes: { id: 0, generation: 0, ageGroup: 'ADULT' },
        children: gen1Children.map(toTreeData),
      };
    }

    // Find the selected branch and show it as root with all descendants
    const branch = branches.find(b => String(b.id) === selectedBranch);
    if (!branch) return null;

    // Build a mini-tree: branch parent as root with their children
    return toTreeData(branch);
  }, [rawData, selectedBranch, branches]);

  const renderNode = useCallback(({ nodeDatum, toggleNode }: CustomNodeElementProps) => {
    const gen = nodeDatum.attributes?.generation ?? 0;
    const color = generationColor(gen as number);
    const isFounder = gen === 0 || gen === '0';
    const isGen1 = gen === 1 || gen === '1';
    const name = displayName(nodeDatum.name);

    if (isFounder) {
      const photoSize = 120;
      const r = photoSize / 2;
      return (
        <g onClick={toggleNode} style={{ cursor: 'pointer' }}>
          <defs>
            <clipPath id="founder-photo-clip">
              <circle cx={0} cy={0} r={r} />
            </clipPath>
          </defs>
          {/* Name above */}
          <text
            fill="#2c3e6b"
            textAnchor="middle"
            x={0}
            y={-r - 16}
            style={{ fontSize: '22px', fontWeight: 700 }}
          >
            {name}
          </text>
          {/* Photo circle */}
          <circle cx={0} cy={0} r={r + 4} fill={color} style={{ filter: 'drop-shadow(0 4px 8px rgba(0,0,0,0.35))' }} />
          <image
            href="/founders.jpg"
            x={-r}
            y={-r}
            width={photoSize}
            height={photoSize}
            clipPath="url(#founder-photo-clip)"
            preserveAspectRatio="xMidYMid slice"
          />
        </g>
      );
    }

    return (
      <g>
        <circle
          r={isGen1 ? 14 : 8}
          fill={color}
          stroke="#fff"
          strokeWidth={2}
          onClick={toggleNode}
          style={{ cursor: 'pointer', filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.2))' }}
        />
        <text
          fill={color}
          strokeWidth={0}
          x={isGen1 ? 20 : 14}
          y={5}
          className="node-name"
          style={{
            fontSize: isGen1 ? '13px' : '11px',
            fontWeight: isGen1 ? 600 : 500,
          }}
        >
          {name}
        </text>
      </g>
    );
  }, []);

  const fitToScreen = useCallback(() => {
    const container = containerRef.current;
    if (!container) return;
    const treeGroup = container.querySelector('.rd3t-g') as SVGGElement | null;
    if (!treeGroup) {
      const { width } = container.getBoundingClientRect();
      setTranslate({ x: width / 2, y: 40 });
      return;
    }
    const bbox = treeGroup.getBBox();
    const { width: cw, height: ch } = container.getBoundingClientRect();
    const padding = 40;
    const zoomX = (cw - padding * 2) / bbox.width;
    const zoomY = (ch - padding * 2) / bbox.height;
    const newZoom = Math.min(zoomX, zoomY, 2);
    const centerX = cw / 2 - (bbox.x + bbox.width / 2) * newZoom;
    const centerY = padding - bbox.y * newZoom;
    setZoom(newZoom);
    setTranslate({ x: centerX, y: centerY });
  }, []);

  // Re-fit when tree data or selected branch changes
  useEffect(() => {
    if (!treeData) return;
    const timer = setTimeout(fitToScreen, 400);
    return () => clearTimeout(timer);
  }, [treeData, treeKey, fitToScreen]);

  useEffect(() => {
    const handleResize = () => fitToScreen();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [fitToScreen]);

  const handleSelectBranch = (id: string) => {
    setSelectedBranch(id);
    setTreeKey(k => k + 1);
  };

  const handleZoomIn = () => setZoom((z) => Math.min(z + 0.15, 2));
  const handleZoomOut = () => setZoom((z) => Math.max(z - 0.15, 0.1));

  if (error && !rawData) return <div className="family-tree-error">Error loading family tree: {error}</div>;
  if (!rawData || !treeData) return (
    <div className="family-tree-page">
      <div className="family-tree-header">
        <Skeleton width="250px" height="1.6rem" style={{ margin: '0 auto 0.5rem' }} />
        <Skeleton width="200px" height="1rem" style={{ margin: '0 auto 0.5rem' }} />
        <Skeleton width="120px" height="1rem" style={{ margin: '0 auto' }} />
      </div>
      <Skeleton width="100%" height="500px" style={{ borderRadius: '12px' }} />
    </div>
  );

  return (
    <div className="family-tree-page">
      <div className="family-tree-header">
        <h2>Tumblin Family Tree</h2>
        <p>Wesley & Esther Tumblin, est. 1948</p>
        <p className="member-count">{rawData.totalMembers} family members</p>
        <p className="edit-hint"><Link to="/members">Manage members</Link></p>
        <div className="legend">
          <span className="legend-item"><span className="dot dot-gold" /> Founders</span>
          <span className="legend-item"><span className="dot dot-blue" /> Gen 1</span>
          <span className="legend-item"><span className="dot dot-red" /> Gen 2</span>
          <span className="legend-item"><span className="dot dot-teal" /> Gen 3</span>
          <span className="legend-item"><span className="dot dot-purple" /> Gen 4</span>
        </div>
      </div>

      <div className="branch-tabs">
        <button
          className={`branch-tab ${selectedBranch === 'overview' ? 'branch-tab-active' : ''}`}
          onClick={() => handleSelectBranch('overview')}
        >
          Overview
        </button>
        {branches.map((b) => (
          <button
            key={b.id}
            className={`branch-tab ${selectedBranch === String(b.id) ? 'branch-tab-active' : ''}`}
            style={{ '--tab-color': getBranchColor(branchKey(b.name)) } as React.CSSProperties}
            onClick={() => handleSelectBranch(String(b.id))}
          >
            {displayName(b.name).split(' ')[0]}
            <span className="branch-tab-count">{countNodes(b)}</span>
          </button>
        ))}
      </div>

      <div className="family-tree-wrapper">
        <div className="zoom-controls">
          <button onClick={handleZoomIn} title="Zoom in">+</button>
          <button onClick={fitToScreen} title="Reset view">Fit</button>
          <button onClick={handleZoomOut} title="Zoom out">-</button>
        </div>
        <div className="family-tree-container" ref={containerRef}>
          <Tree
            key={treeKey}
            data={treeData}
            orientation="vertical"
            translate={translate}
            zoom={zoom}
            scaleExtent={{ min: 0.1, max: 2 }}
            pathFunc="step"
            separation={{ siblings: 1, nonSiblings: 1.2 }}
            nodeSize={{ x: 160, y: selectedBranch === 'overview' ? 220 : 90 }}
            renderCustomNodeElement={renderNode}
            collapsible
            initialDepth={selectedBranch === 'overview' ? 1 : undefined}
          />
        </div>
      </div>
    </div>
  );
}
