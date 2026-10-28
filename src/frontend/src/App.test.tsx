import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';

describe('App shell', () => {
  it('shows the product name so users know which system they are in', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: 'e-HoSo' })).toBeInTheDocument();
  });
});
