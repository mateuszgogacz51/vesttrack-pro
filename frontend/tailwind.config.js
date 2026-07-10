/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Steel blue - kolor podstawowy (primary): przyciski, linki, nawigacja, wykresy Core
        brand: {
          DEFAULT: '#36597A',
          dark: '#243D54',
          light: '#4C7299',
          50: '#EEF2F6'
        },
        // Warm clay - kolor akcentu (secondary): wyroznienia, Satellite w alokacji, hover states
        clay: {
          DEFAULT: '#CFA58C',
          dark: '#B8875F',
          light: '#E1C4AE'
        },
        ink: {
          DEFAULT: '#1B2C3D',
          light: '#243D54'
        },
        paper: '#F7F6F4',
        slate: {
          DEFAULT: '#475569',
          light: '#94A3B8'
        },
        crimson: {
          DEFAULT: '#B91C1C',
          light: '#EF4444'
        }
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        body: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      },
      borderRadius: {
        card: '10px'
      },
      boxShadow: {
        card: '0 1px 2px 0 rgba(27,44,61,0.06), 0 1px 3px 0 rgba(27,44,61,0.08)'
      }
    }
  },
  plugins: []
}
