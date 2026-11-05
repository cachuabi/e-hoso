import { describe, expect, it } from 'vitest';
import { DEADLINE_TEXT, fileProblem, formatDateTime, MAX_FILE_BYTES, statusTone } from './labels';

describe('labels', () => {
  it('[FR-24] [NFR-06] deadline labels carry words, not only colour', () => {
    expect(DEADLINE_TEXT.OVERDUE.text).toBe('Quá hạn');
    expect(DEADLINE_TEXT.DUE_SOON.text).toBe('Sắp đến hạn');
    expect(DEADLINE_TEXT.ON_TIME.text).toBe('Còn hạn');
  });

  it('[FR-13] status tones separate problems from results', () => {
    expect(statusTone('REJECTED')).toBe('bad');
    expect(statusTone('RETURNED')).toBe('ok');
    expect(statusTone('IN_PROGRESS')).toBe('info');
  });

  it('[NFR-10] shows times in the Vietnam time zone', () => {
    expect(formatDateTime('2026-11-05T10:00:00Z')).toContain('17:00');
    expect(formatDateTime(null)).toBe('–');
  });

  it('[FR-08] checks files before uploading', () => {
    expect(fileProblem('a.exe', 10)).toContain('PDF');
    expect(fileProblem('a.PDF', MAX_FILE_BYTES + 1)).toContain('10 MB');
    expect(fileProblem('a.png', 100)).toBeNull();
  });
});
