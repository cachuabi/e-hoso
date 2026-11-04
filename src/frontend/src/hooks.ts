import { useCallback, useEffect, useState } from 'react';
import { errorText } from './api';

/** Loads data on mount and whenever `key` changes; `reload` fetches again. */
export function useLoad<T>(loader: () => Promise<T>, key: string) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick((t) => t + 1), []);
  useEffect(() => {
    let alive = true;
    loader()
      .then((value) => {
        if (alive) {
          setData(value);
          setError(null);
        }
      })
      .catch((e: unknown) => {
        if (alive) setError(errorText(e));
      });
    return () => {
      alive = false;
    };
    // The loader closes over the values encoded in `key`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, tick]);
  return { data, error, reload };
}

/** Runs one action at a time and keeps its error. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, run };
}
