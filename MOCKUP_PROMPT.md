# Промпт для генерации макетов UI

---

## 🤖 Промпт для Figma Agent (основной)

Вставь этот промпт целиком в Figma Agent. Он создаст весь проект — мобильное приложение и веб-редактор — в одном Figma-файле с единой дизайн-системой.

```
Create a complete Figma design project called "Brandkit AI" for an AI-powered brandbook generator service.
The product lets users generate company brand identity elements (logos, avatars, chat covers, business cards)
from text descriptions using an LLM, edit them in a vector editor, and publish to a public feed.

---

## DESIGN SYSTEM (create as a shared "🎨 Design System" page)

Color palette:
- Background: #0F0F14 (dark base)
- Surface: #1A1A24 (cards, panels)
- Surface elevated: #22222F (modals, bottom sheets)
- Accent primary: #7C3AED (electric violet)
- Accent secondary: #06B6D4 (cyan)
- Accent gradient: linear-gradient(135deg, #7C3AED → #06B6D4)
- Text primary: #F4F4F5
- Text secondary: #A1A1AA
- Text muted: #52525B
- Success: #22C55E
- Error: #EF4444
- Border: #27272A

Typography (Inter font):
- H1: 32px Bold
- H2: 24px SemiBold
- H3: 18px SemiBold
- Body: 15px Regular
- Caption: 12px Regular
- Label: 13px Medium

Components to define:
- Primary Button (violet, 48px tall, 16px radius, full-width and auto-width variants)
- Secondary Button (ghost, violet border)
- Text Input (dark fill, 12px radius, label above, error state)
- Card / Project Card (dark surface, 16px radius, thumbnail + author row + title)
- Avatar (circle, sizes: 24px, 40px, 56px)
- Tab Bar item (active = accent, inactive = muted)
- Bottom Navigation Bar (4 tabs: Feed, Create, Profile + icons)
- AI Chat bubble — User (right-aligned, violet bg)
- AI Chat bubble — AI (left-aligned, surface elevated bg, optional thumbnail inside)
- Version row (thumbnail + metadata + Restore button)
- Skeleton loader (animated placeholder variant for card and list item)
- Badge / Tag (pill shape, 6px radius)

---

## PAGE 1: "📱 Mobile App" — iPhone 14 Pro frames (393 × 852 pt)

Arrange all mobile frames in a horizontal flow, grouped by section with section labels above.

### Section: Onboarding

Frame "Onboarding 1 — Welcome":
- Full-screen gradient background (#7C3AED → #06B6D4)
- Centered logo mark: abstract "B" letterform with gradient stroke
- App name "Brandkit AI" in H1 white
- Tagline "Turn words into brand identity" in Body text-secondary
- Small dot progress indicator (3 dots, first active)
- No navigation bar

Frame "Onboarding 2 — How it works":
- Dark background
- 4 steps in vertical list, each with a small icon and short description:
  1. "Describe" — write what you need
  2. "Generate" — AI creates vector graphics
  3. "Edit" — refine in the visual editor
  4. "Publish" — share with the world
- Gradient accent icons for each step
- Progress dots (2nd active)

Frame "Onboarding 3 — Get Started":
- Dark background
- Large illustration area: mockup of the app showing a generated logo
- "Start creating for free" heading H2
- Primary Button "Get Started" full-width
- Text link "Sign in" below
- Progress dots (3rd active)

---

### Section: Auth

Frame "Registration":
- App logo small at top
- H2 "Create account"
- Inputs: Full name, Email, Password (show/hide toggle)
- Primary Button "Create account"
- Divider "or"
- Secondary Button "Continue with Google" (Google icon)
- Footer link "Already have an account? Sign in"

Frame "Login":
- Same layout as Registration but: H2 "Sign in", fields: Email + Password, primary button "Sign in"
- "Forgot password?" link
- Footer link "No account? Register"

---

### Section: Feed

Frame "Feed — All (filled)":
- Status bar at top
- Header: "Brandkit AI" logo left, search icon right
- Tab bar: "All" (active, violet underline) | "Subscriptions"
- 2-column grid of Project Cards (show 6 cards, various brand previews as colored placeholder rectangles with gradients)
- Bottom Navigation Bar (Feed tab active)

Frame "Feed — Subscriptions (empty state)":
- Same header and tab bar, "Subscriptions" tab active
- Centered empty state illustration (simple icon of people with "+" sign)
- Text: "No subscriptions yet"
- Subtext: "Follow creators to see their work here"
- Primary Button "Explore all projects"
- Bottom Navigation Bar (Feed tab active)

Frame "Feed — All (loading skeleton)":
- Same structure as filled feed but all cards replaced with Skeleton loader components

---

### Section: Profile

Frame "Profile — Own":
- Cover area (gradient bar at top, 120px tall)
- Avatar (56px) overlapping cover bottom edge
- Username bold, @handle muted below
- Bio text (1–2 lines)
- Stats row: "12 Projects · 340 Followers · 28 Following" (tappable, separated by dots)
- "Edit Profile" secondary button
- Section label "Projects"
- 2-column grid of Project Cards (show 4 cards)
- Bottom Navigation Bar (Profile tab active)

Frame "Profile — Other user":
- Same layout but:
  - Replace "Edit Profile" with "Follow" primary button
  - Show follower count as if not following yet
  - No edit affordances

---

### Section: Create & Edit Project

Frame "Create Project — Empty":
- Back arrow top left, title "New Project" center
- Input "Project name" (e.g. "My Startup Brand")
- Large multiline text area "Describe your brand... style, colors, industry, mood"
- Helper text below: "Be specific — mention colors, shapes, industry, mood"
- Primary Button "Generate →" at bottom (full width, gradient background)

Frame "Create Project — Generating (loading)":
- Same layout, button replaced with loading spinner + "Generating..." label
- Text area disabled / greyed out
- Animated gradient shimmer on the preview area placeholder

Frame "Edit Project — With result":
- Title bar: project name editable field + "···" overflow menu (right)
- WebView area placeholder (large card, ~50% of screen height): shows a placeholder colored block with label "Vector Editor (WebView)" in center, subtle border
- Divider with label "AI Chat"
- 2–3 AI chat bubbles showing conversation history (user asked for changes, AI responded with update)
- Chat input bar at bottom: text field "Describe changes..." + Send button (violet arrow icon)
- Top right badges: lock icon = "Private" pill; share icon

Frame "Edit Project — Visibility toggle":
- Same as above but a modal bottom sheet is overlaid
- Bottom sheet title "Visibility"
- Two options as large tappable rows:
  - 🔒 Private — "Only you can see this"
  - 🌐 Public — "Visible in feed and your profile"
- Currently selected option has a violet checkmark

---

### Section: AI Chat

Frame "AI Chat — Conversation":
- Nav bar: back arrow, project name as title, history icon (right)
- Chat timeline (top to bottom, newest at bottom):
  - System message: "Project created"
  - User bubble: "Make the background darker and add a subtle grid pattern"
  - AI bubble with thumbnail preview inside: "Done! I've updated the background to #0D0D14 with a subtle dot grid."
  - User bubble: "Change the font to something more geometric"
  - AI bubble with thumbnail: "Updated to Geist Mono. Here's the new version:"
- Input bar: text field + Send button
- Thin "Version saved" label between AI responses and next user message

---

### Section: Version History

Frame "Version History":
- Nav bar: back arrow, "History" title, optional filter icon
- Vertical list of Version rows:
  - Version 4 (current) — highlighted with violet left border: thumbnail + "AI Chat · 2 min ago" + "Restore" button (greyed out = current)
  - Version 3 — thumbnail + "AI Chat · 15 min ago" + "Restore" outline button
  - Version 2 — thumbnail + "Manual edit · 1 hr ago" + "Restore" outline button
  - Version 1 — thumbnail + "Generated · 2 hrs ago" + "Restore" outline button (original, badge "Original")
- Each row: 56px thumbnail (small gradient rect), version number bold, source + time in caption, right-side action

---

### Section: Public Project View

Frame "Public Project — View":
- No auth required state (shown without bottom nav)
- Hero area: full-width project thumbnail (tall, ~300px)
- Author row: avatar + name + "Follow" button
- Project title H2
- Published date caption
- "Download PNG" primary button
- "Download SVG" secondary button
- Separator
- "More from this creator" section: 2-column mini grid of that user's other public projects

---

## PAGE 2: "🖥 Web Editor" — Desktop frames (1440 × 900 pt)

### Frame "Editor — Empty State":
- Light theme (background #F9FAFB)
- Top bar: app logo left, "Sign in" and "Get Started" buttons right
- Center of canvas: empty placeholder with dashed border (800×600 area)
- Inside: icon + "Paste your description and generate" text
- Below placeholder: large text input area + "Generate →" primary button (violet)

### Frame "Editor — Generated Result":
- Top toolbar (48px tall, light gray #F3F4F6):
  - Left: app logo, project name editable text field
  - Center: tool icons — Select (arrow), Move (hand), Zoom in/out, Undo, Redo
  - Right: "Save version" outline button, "Export ▾" primary button, user avatar
- Left sidebar (240px, white, border-right):
  - Section label "Layers"
  - Tree list of SVG elements: Group "Logo", │ Rect "background", │ Path "icon-shape", │ Text "Company Name", │ Text "Tagline"
  - Selected item "Path icon-shape" highlighted in violet
- Center canvas (dark checkerboard bg):
  - White artboard 600×600px centered
  - Generated SVG rendered: abstract logo with geometric shape + company name text below
  - Selected element "Path icon-shape" has blue selection handles (8 corner + edge handles)
- Right sidebar (280px, white, border-left):
  - Section "Fill": color swatch (#7C3AED) + opacity slider
  - Section "Stroke": color swatch (none) + weight input
  - Section "Position": X, Y number inputs
  - Section "Size": W, H number inputs with lock-ratio icon
  - Section "Corner radius": input field
- Bottom status bar: "1 element selected · 600×600 artboard · 100%"

### Frame "Editor — Context Menu":
- Same as Generated Result but:
  - Right-click context menu visible over canvas: "Cut, Copy, Paste, Delete, Bring to Front, Send to Back, Group"
  - Menu positioned near the selected element

### Frame "Editor — Export Panel":
- Same as Generated Result but "Export ▾" button is clicked, dropdown panel visible:
  - "Export as PNG" with size options (512×512, 1024×1024, Custom)
  - "Export as SVG"
  - "Export as PDF" (greyed out, "Coming soon" badge)

### Frame "Editor — Mobile / iframe (393pt wide)":
- Compact top bar: back arrow, project name, "···" menu
- Canvas takes full width, artboard centered and scaled to fit
- Selected element shows touch-friendly handles (larger hit targets)
- Collapsed bottom sheet (pull-up tab visible at bottom):
  - Tab label "Properties ▲"
  - When expanded: same Fill, Stroke, Position, Size fields in 2-column grid
- Floating mini toolbar (4 icons): Select, Undo, Redo, Zoom-to-fit

---

## PAGE 3: "📐 Component Library" — auto-layout grid

On this page, display all defined components from the Design System section in an organized grid:
- Each component in its own labeled frame
- Show Default, Hover, Active, Disabled states side by side
- Show Dark theme variants (all mobile components)
- Show Light theme variants (all web editor components)

---

## FINAL NOTES

- All frames should use Auto Layout where appropriate
- Apply consistent 16px and 24px spacing grid
- Use real-looking placeholder content (company names like "Orion Labs", "Nova Studio", logos as gradient geometric shapes)
- Apply drop shadows to elevated surfaces (cards, modals, sidebars)
- All interactive elements should have visible hover/active states defined
- Name all layers and frames clearly in English using the exact names from this prompt
```

---

Используй этот промпт в инструментах генерации макетов: **v0.dev**, **Galileo AI**, **Uizard**, **Figma AI**, или любом другом LLM/image-generation инструменте.

---

## Промпт (английский — рекомендован для большинства инструментов)

```
Design a mobile app UI for a brandbook generator service powered by AI.
The app allows users to create, edit, and publish company brand identity kits
(logos, avatars, chat covers, business cards) generated by an LLM from text descriptions.

Design the following screens in a consistent style:

1. **Onboarding (3 screens)**
   - Welcome screen with product name "Brandkit AI" and hero illustration of vector graphics being generated
   - Features overview: "Describe → Generate → Edit → Publish"
   - Call-to-action: "Get Started" button

2. **Registration / Login screen**
   - Email + password fields
   - "Sign in with Google" button
   - Toggle between Login and Register modes

3. **Feed screen (main screen)**
   - Top tab bar: "All" | "Subscriptions"
   - Masonry/grid layout of brand project cards
   - Each card: project preview thumbnail, author avatar + name, project name, date
   - Bottom navigation bar: Feed, Create, Profile

4. **Create / Edit Project screen**
   - Top: project name editable field
   - Center: preview area showing the generated SVG/vector result (embedded WebView area placeholder)
   - Bottom panel: chat-style input field with "Send to AI" button (for describing changes)
   - Floating action button: "Regenerate"
   - Top right: "Publish" / "Save" button
   - Visibility toggle: Private / Public

5. **AI Chat screen (inside a project)**
   - Chat bubbles (user messages on the right, AI responses on the left)
   - AI response includes a preview of the updated brandbook element
   - Input field at bottom: "Describe your changes..."
   - History of all AI edits in the chat timeline

6. **Version History screen**
   - List of versions: version number, date, source (AI Chat / Manual Edit)
   - Each row: small thumbnail preview of that version
   - "Restore" button on each row
   - Currently active version is highlighted

7. **User Profile screen**
   - Avatar, username, bio
   - Stats row: Projects | Followers | Following
   - "Follow" button (on other users' profiles)
   - Grid of public projects (same card style as feed)
   - Own profile has "Edit Profile" button instead of "Follow"

**Design style:**
- Modern, minimal, dark theme with vibrant accent color (electric violet or deep teal)
- Clean typography: bold headings, regular body text
- Rounded corners on cards and buttons
- Smooth gradient backgrounds on hero/onboarding screens
- Mobile-first (375×812 pt, iPhone frame)
- Material Design 3 or iOS HIG conventions
- Show all interactive states: active, empty, loading skeleton

Output: high-fidelity mockups for all screens, consistent color palette and component library.
```

---

## Промпт для веб-редактора (фронтенд)

```
Design a web-based vector graphics editor UI (desktop, 1440px wide) for an AI brandbook generator.
The editor is embedded as an iframe inside a mobile app and used as a standalone web tool.

**Screens / states to design:**

1. **Editor — empty state**
   - Centered placeholder: "Generate your first brandbook element"
   - Large text input area: "Describe what you want to create..."
   - "Generate" primary button

2. **Editor — generated result**
   - Left sidebar: layer panel (list of SVG elements/groups)
   - Center canvas: the generated SVG rendered and editable (with selection handles visible on selected element)
   - Right sidebar: properties panel (fill color, stroke, opacity, position X/Y, width/height, font settings)
   - Top toolbar: Select, Move, Scale, Rotate tools; Undo/Redo; Zoom controls
   - Bottom bar: "Export PNG" button, "Export SVG" button, "Save version" button

3. **Editor — element selected state**
   - One element on canvas has blue selection handles
   - Right panel shows that element's properties filled in
   - Context menu visible (Cut, Copy, Delete, Bring to front)

4. **Editor — mobile-optimized (375px wide, iframe mode)**
   - Simplified toolbar (icon-only, collapsible)
   - Touch-friendly handles on canvas
   - Properties panel collapses to bottom sheet

**Design style:**
- Light theme (white canvas, light gray panels)
- Figma/Canva-inspired layout
- Accent color: electric violet (#7C3AED) for primary actions
- Clean, professional, distraction-free
```

---

## Промпт для Figma AI / FigJam (на русском)

```
Создай макеты мобильного приложения «Brandkit AI» — генератор брендбуков на основе ИИ.

Приложение позволяет:
- Создавать элементы фирменного стиля (логотипы, аватарки, обложки чатов) по текстовому описанию через LLM
- Редактировать результат через чат с ИИ или вручную в векторном редакторе
- Публиковать проекты в общую ленту и профиль

Экраны для дизайна:
1. Онбординг (3 экрана) — знакомство с продуктом
2. Регистрация / Вход
3. Лента — общая и лента подписок (переключение вкладками)
4. Создание/редактирование проекта — область превью + чат с ИИ снизу
5. История изменений — список версий с превью и кнопкой восстановления
6. Профиль пользователя — своя страница и чужая

Стиль:
- Тёмная тема, акцентный цвет — электрический фиолетовый или темно-бирюзовый
- Скруглённые углы, минималистичный дизайн
- Формат: iPhone 14 Pro (393×852 pt)
- Консистентная компонентная библиотека

Покажи все состояния: загрузка (skeleton), пустое состояние, заполненное, ошибка.
```

---

## Рекомендуемые инструменты

| Инструмент | Ссылка | Тип промпта |
|---|---|---|
| v0.dev (Vercel) | https://v0.dev | Текстовый, генерирует React-компоненты |
| Galileo AI | https://www.usegalileo.ai | Текстовый, генерирует Figma-макеты |
| Uizard | https://uizard.io | Текстовый + скетчи |
| Figma AI | внутри Figma | Текстовый |
| Midjourney / DALL·E | — | Для иллюстраций и онбординга |

---

## Список экранов для трекинга

- [ ] Онбординг 1 — Welcome
- [ ] Онбординг 2 — Features
- [ ] Онбординг 3 — CTA
- [ ] Регистрация
- [ ] Вход
- [ ] Лента — общая (заполненная)
- [ ] Лента — подписки (пустая)
- [ ] Профиль — свой
- [ ] Профиль — чужой
- [ ] Создание проекта — пустое состояние
- [ ] Создание проекта — генерация (loading)
- [ ] Редактирование проекта — с результатом
- [ ] Чат с ИИ — история сообщений
- [ ] История изменений
- [ ] Веб-редактор — пустой
- [ ] Веб-редактор — с результатом
- [ ] Веб-редактор — мобильная версия (iframe)
