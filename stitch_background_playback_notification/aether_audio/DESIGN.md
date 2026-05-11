---
name: Aether Audio
colors:
  surface: '#fbf8ff'
  surface-dim: '#dbd8e5'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f2ff'
  surface-container: '#efecf9'
  surface-container-high: '#e9e7f3'
  surface-container-highest: '#e3e1ed'
  on-surface: '#1b1b24'
  on-surface-variant: '#464555'
  inverse-surface: '#303039'
  inverse-on-surface: '#f2effc'
  outline: '#777587'
  outline-variant: '#c7c4d8'
  surface-tint: '#4d44e3'
  primary: '#3525cd'
  on-primary: '#ffffff'
  primary-container: '#4f46e5'
  on-primary-container: '#dad7ff'
  inverse-primary: '#c3c0ff'
  secondary: '#58579b'
  on-secondary: '#ffffff'
  secondary-container: '#b6b4ff'
  on-secondary-container: '#444386'
  tertiary: '#474949'
  on-tertiary: '#ffffff'
  tertiary-container: '#5f6061'
  on-tertiary-container: '#dbdbdb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3322cc'
  secondary-fixed: '#e2dfff'
  secondary-fixed-dim: '#c2c1ff'
  on-secondary-fixed: '#130f54'
  on-secondary-fixed-variant: '#403f81'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c7'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#fbf8ff'
  on-background: '#1b1b24'
  surface-variant: '#e3e1ed'
typography:
  display:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  h1:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  h2:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  safe-margin: 20px
  gutter: 12px
---

## Brand & Style

This design system centers on a "Translucent Clarity" aesthetic, blending modern minimalism with glassmorphism. The brand personality is professional, efficient, and forward-thinking, utilizing a **Vibrant Indigo** core to reduce the cognitive load of converting and managing media while maintaining a high-energy feel.

The visual style utilizes soft background blurs and semi-transparent layers to maintain a sense of depth and context. It avoids heavy shadows in favor of light-refractive borders and subtle tonal shifts, creating a UI that feels like digital glassware—polished, premium, and unobtrusive.

## Colors

The palette is anchored by **Vibrant Indigo** (#6760fd) for primary actions and key navigation elements, evoking a sense of power and modern digital precision. **Muted Periwinkle** (#7170b6) serves as the secondary accent, used for supporting UI elements and active selection states to provide a grounding, trustworthy contrast. 

The system utilizes a **Pure White** (#ffffff) tertiary color to emphasize the glassmorphic aesthetic, serving as a high-brightness highlight for surface glints or stark, high-contrast interactive states. The background is a crisp neutral to ensure the glassmorphic elements have enough contrast to appear distinct. Text utilizes a cool-toned slate neutral to maintain high readability without the harshness of pure black.

## Typography

This design system employs **Inter** across all levels to leverage its exceptional legibility and neutral, modern character. 

The hierarchy is strictly enforced through weight and scale. Display and Headline styles use a semi-bold weight with tighter letter spacing to create a compact, professional look for track titles and screen headers. Body text remains generous in line-height to ensure metadata and descriptions are easily scannable during mobile use.

## Layout & Spacing

The layout follows a fluid-width model tailored for mobile devices, using a **4px base unit** for all measurements. 

A standard **20px safe-margin** is applied to the left and right of the viewport. Components like video cards are arranged in a single-column list with **12px gutters**. The navigation bar is detached from the screen edges, floating at the bottom with a 16px margin from the base to reinforce the glassmorphic, layered concept.

## Elevation & Depth

Depth is achieved through **Backdrop Blurs** and **Low-Contrast Outlines** rather than traditional shadows. 

1.  **Level 0 (Base):** The primary background color.
2.  **Level 1 (Cards):** Solid neutral surfaces with a 1px stroke.
3.  **Level 2 (Glass):** Used for the navigation bar and modal overlays. These feature a `backdrop-filter: blur(20px)`, 70% opacity white fill, and a subtle 1px inner border in pure white (20% opacity) to simulate a "glint" on the glass edge.
4.  **Level 3 (Controls):** Floating Action Buttons (FAB) utilize a soft, tinted shadow (using the primary indigo color at 15% opacity) to signal interactability.

## Shapes

The shape language is consistently **Rounded**, reflecting a modern and approachable brand attribute. 

Standard components like cards and buttons use a **16px (1rem) radius**. The floating navigation bar and the "Play" action buttons utilize a **Pill-shape (fully rounded)** execution to distinguish them as high-priority interactive elements. Icon containers and progress bars also follow this pill-shaped convention to maintain visual harmony.

## Components

### Navigation Bar
The centerpiece of the UI. A floating pill-shaped bar using the glassmorphism style defined in the Elevation section. Icons are line-style with a 2px stroke width. The active state is indicated by a vibrant indigo glow behind the icon.

### Video & Playlist Cards
Low-profile containers with a 1px subtle border. They feature a 16:9 thumbnail on the left for videos, or a stacked-square aesthetic for playlists. Titles are H2, metadata (duration, file size) uses Label-SM.

### Floating Action Button (FAB)
A circular button with a Vibrant Indigo fill. It houses the primary conversion icon. It should sit slightly above the glass nav bar on the right side of the screen.

### Play Controls
The "Now Playing" bar uses a simplified glassmorphic strip. The play/pause toggle is a prominent, large pill-shaped button using the Muted Periwinkle palette. Progress bars are thin, using a Vibrant Indigo indicator and a neutral-tinted track.

### Icons
Line-style only. 
- **Library:** Folder icon with a small video play symbol inside.
- **Playlists:** List icon with a musical note offset.
- **Settings:** Minimalist six-tooth gear icon.