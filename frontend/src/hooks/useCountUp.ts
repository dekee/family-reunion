import { useEffect, useState } from 'react';

export function useCountUp(target: number, duration = 1200, start = false): number {
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (!start || target === 0) {
      if (start) setValue(target);
      return;
    }

    let startTime: number | null = null;
    let animationId: number;

    function animate(timestamp: number) {
      if (!startTime) startTime = timestamp;
      const elapsed = timestamp - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(Math.round(eased * target));

      if (progress < 1) {
        animationId = requestAnimationFrame(animate);
      }
    }

    animationId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationId);
  }, [target, duration, start]);

  return value;
}
