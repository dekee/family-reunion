import { useEffect, useState, useCallback, useRef } from 'react';
import Tree from 'react-d3-tree';
import type { RawNodeDatum, CustomNodeElementProps } from 'react-d3-tree';
import { fetchFamilyTree } from '../api';
import type { FamilyTreeNode } from '../types';
import './FamilyTree.css';

function toTreeData(node: FamilyTreeNode): RawNodeDatum {
  return {
    name: node.name,
    attributes: {
      generation: node.generation ?? 0,
      ageGroup: node.ageGroup,
    },
    children: node.children.map(toTreeData),
  };
}

function generationColor(gen: number | string): string {
  const g = typeof gen === 'string' ? parseInt(gen, 10) : gen;
  if (g === 0) return '#c9a84c'; // gold — founders
  if (g === 1) return '#2c3e6b'; // navy — children
  return '#c0392b'; // red — grandchildren (Gen 2+)
}

function renderNode({ nodeDatum, toggleNode }: CustomNodeElementProps) {
  const gen = nodeDatum.attributes?.generation ?? 0;
  const color = generationColor(gen as number);
  const isFounder = gen === 0 || gen === '0';

  return (
    <g>
      <circle
        r={isFounder ? 16 : 10}
        fill={color}
        stroke="#333"
        strokeWidth={1.5}
        onClick={toggleNode}
        style={{ cursor: 'pointer' }}
      />
      <text
        fill={color}
        strokeWidth={0}
        x={isFounder ? 22 : 16}
        y={4}
        style={{ fontSize: isFounder ? '12px' : '10px', fontWeight: isFounder ? 700 : 500 }}
      >
        {nodeDatum.name}
      </text>
    </g>
  );
}

export default function FamilyTree() {
  const [treeData, setTreeData] = useState<RawNodeDatum | null>(null);
  const [totalMembers, setTotalMembers] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [translate, setTranslate] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(0.55);

  useEffect(() => {
    fetchFamilyTree()
      .then((res) => {
        setTotalMembers(res.totalMembers);
        if (res.roots.length === 1) {
          setTreeData(toTreeData(res.roots[0]));
        } else {
          const virtualRoot: RawNodeDatum = {
            name: 'Wesley & Esther Tumblin',
            attributes: { generation: 0, ageGroup: 'ADULT' },
            children: res.roots.map(toTreeData),
          };
          setTreeData(virtualRoot);
        }
      })
      .catch((err) => setError(err.message));
  }, []);

  const updateTranslate = useCallback(() => {
    if (containerRef.current) {
      const { width } = containerRef.current.getBoundingClientRect();
      setTranslate({ x: width / 2, y: 40 });
    }
  }, []);

  useEffect(() => {
    updateTranslate();
    window.addEventListener('resize', updateTranslate);
    return () => window.removeEventListener('resize', updateTranslate);
  }, [updateTranslate]);

  const handleZoomIn = () => setZoom((z) => Math.min(z + 0.15, 2));
  const handleZoomOut = () => setZoom((z) => Math.max(z - 0.15, 0.1));
  const handleZoomReset = () => {
    setZoom(0.55);
    updateTranslate();
  };

  if (error) return <div className="family-tree-error">Error loading family tree: {error}</div>;
  if (!treeData) return <div className="family-tree-loading">Loading family tree...</div>;

  return (
    <div className="family-tree-page">
      <div className="family-tree-header">
        <h2>Tumblin Family Tree</h2>
        <p>Wesley & Esther Tumblin, est. 1948</p>
        <p className="member-count">{totalMembers} family members</p>
        <div className="legend">
          <span className="legend-item"><span className="dot dot-gold" /> Founders</span>
          <span className="legend-item"><span className="dot dot-blue" /> Children (Gen 1)</span>
          <span className="legend-item"><span className="dot dot-red" /> Grandchildren (Gen 2)</span>
        </div>
      </div>
      <div className="family-tree-wrapper">
        <div className="zoom-controls">
          <button onClick={handleZoomIn} title="Zoom in">+</button>
          <button onClick={handleZoomReset} title="Reset view">Fit</button>
          <button onClick={handleZoomOut} title="Zoom out">-</button>
        </div>
        <div className="family-tree-container" ref={containerRef}>
          <Tree
            data={treeData}
            orientation="vertical"
            translate={translate}
            zoom={zoom}
            scaleExtent={{ min: 0.1, max: 2 }}
            pathFunc="step"
            separation={{ siblings: 1, nonSiblings: 1.2 }}
            nodeSize={{ x: 150, y: 80 }}
            renderCustomNodeElement={renderNode}
            collapsible
            initialDepth={1}
          />
        </div>
      </div>
    </div>
  );
}
