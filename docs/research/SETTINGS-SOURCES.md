# Sources for a TV settings findability / IA review

Checked 2026-11-27.

**How each source was checked.**
- **Fetched (F):** the page was opened in full. Its quotes are verbatim and under 30 words.
- **Search only (S):** this session's network blocks the site's host (m3.material.io, www.nngroup.com, source.android.com; w3.org was not tried). The URL appears in live web-search results, but the page was not opened. The text after it is a **paraphrase of the search summary, not a quote**. Open the page and check the wording before you quote it.

---

## 1. Android TV / Google TV design (developer.android.com)

### Navigation on TV (TV design guides) (F)
- URL: https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv
- Date: Last updated 2025-05-09
- "Make it fast and simple to get to content. … Organize your information in a way that requires the fewest screens."
- "Don't over-complicate by adding unnecessary layers of navigation."
- "If a user doesn't have a straight path to get to a control, consider relocating it." / Don't: "Avoid complex and nested layout hierarchies."
- Use it for: keeping settings shallow, giving each D-pad axis one job, keeping every item reachable in a straight line.

### TV navigation (developer training) (F)
- URL: https://developer.android.com/training/tv/get-started/navigation
- Date: Last updated 2026-10-23
- "It is important to always have an item in focus that a user can take action on immediately after your app starts or any time it is idle."
- "Use a uniform scheme for indicating focus across your application."
- "App uses left navigation: activate the left side menu and focus on the currently active menu item." (what Back should do)
- Use it for: a visible focus on every screen, one focus style everywhere, Back that returns to the parent category with focus on it.

### Layouts (TV design guides) (F)
- URL: https://developer.android.com/design/ui/tv/guides/styles/layouts
- Date: Last updated 2025-05-09
- "Position the elements with a 5% margin of 48dp on the left and right sides, and 27dp on the top and bottom of a layout."
- "A 2-pane layout performs better when the page shows hierarchical content. It is widely used on task-forward experiences."
- "Ensure the focus path is aligned with hierarchy or the decision-making logic." / "Avoid using too many panels to group contents."
- Use it for: a two-pane settings screen (categories, then items), overscan-safe margins, a focus path that follows the hierarchy.

### Typography (TV design guides) (F)
- URL: https://developer.android.com/design/ui/tv/guides/styles/typography
- Date: Last updated 2026-07-06
- "Prioritize using larger typography for a more comfortable viewing experience on TV screens."
- "Any text on TV needs to have a legible font width, as thinner lines are not instantly recognisable."
- "Use titles for UI elements like cards or lists."
- Use it for: large, non-thin type for setting titles; no decorative fonts.

### Lists (TV design guides, Compose for TV `ListItem`) (F)
- URL: https://developer.android.com/design/ui/tv/guides/components/lists
- Date: Last updated 2026-03-30
- "Lists should feel natural and be scannable."
- "Avoid placing buttons inside a list item as it can cause confusion about which element is currently in focus."
- "Avoid relying solely on the background color to indicate selection in a list."
- Use it for: one focus target per settings row (title, subtitle, and a switch, radio or checkbox), the current value shown by an icon and not by colour alone, and icons only where they carry meaning.

### Navigation drawer (TV design guides) (F)
- URL: https://developer.android.com/design/ui/tv/guides/components/navigation-drawer
- Date: Last updated 2026-03-30
- "Destinations are ordered according to user importance, with frequent destinations first and related destinations grouped together."
- "Limit the number of main navigation destinations in the navigation drawer to five to six."
- "Use badges sparingly and only when necessary, as they can make the UI appear busy and cluttered." / "Avoid using long text labels that require wrapping."
- Use it for: ordering top-level categories by how often they are used, a small top level, short labels, few badges.

---

## 2. Apple Human Interface Guidelines (tvOS)

Apple's pages load their text with JavaScript, so the text was read from Apple's JSON version of the same page. The URLs below are the pages people open.

### Settings (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/settings
- Date: change log, June 10, 2024
- "Minimize the number of settings you offer. … too many settings can make the experience feel less approachable, while also making it hard to find a particular setting."
- "Aim to provide default settings that give the best experience to the largest number of people."
- "Make these options available in the screens they affect, where they're discoverable and convenient."
- Use it for: fewer settings and good defaults; putting a task's options in context (for example, adhan options next to the adhan) and not only in Settings.

### Focus and selection (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/focus-and-selection
- Date: change log, October 24, 2023
- "Avoid changing focus without people's interaction. People rely on the focus system to help them know where they are in your app."
- "tvOS users rely on using directional gestures … to reach every onscreen element, so you need to make sure that people can bring focus to every element."
- "It's usually easier for people to view lists and collections when an entire row is highlighted."
- Use it for: no focus jumps the person did not cause, every element reachable, the whole row highlighted in a settings list.

### Remotes (tvOS) (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/remotes
- Date: not shown
- "In almost all cases, open the parent of the current screen when people press the Back button."
- "… within an app, the parent is defined by the app hierarchy, and isn't necessarily the previous screen."
- Use it for: Back goes up one level in the settings tree, which makes the depth easy to predict.

### Tab bars (tvOS section) (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/tab-bars
- Date: change log, June 8, 2026
- "Include tab labels to help with navigation. … Use single words whenever possible."
- "Don't disable or hide tab bar buttons, even when their content is unavailable. … If a section is empty, explain why."
- "Reserve badges for critical information so you don't dilute their impact and meaning."
- Use it for: stable, always-visible categories; short labels; badges kept rare.

### Searching (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/searching
- Date: change log, June 8, 2026
- "Aim to make your app's content searchable through a single location."
- "Provide suggestions to make searching easier. … help people search faster and type less."
- Use it for: if settings get search, one entry point with suggestions, because typing with a D-pad is slow.

### Writing (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/writing
- Date: change log, December 16, 2025
- "Create a list of common terms, and reference that list to keep your language consistent."
- "Bigger screens also require brevity, as the text must be large for people to see it from a distance."
- "Prioritize clarity and avoid the temptation to be too cute or clever with your labels."
- Use it for: one glossary of terms (the Arabic setting names), short and plain labels.

### Layout (tvOS section) (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/layout
- Date: change log, September 9, 2026
- "Inset primary content 60 points from the top and bottom of the screen, and 80 points from the sides."
- Use it for: overscan-safe margins, which agree with Android's 5% rule.

### Designing for tvOS (F)
- URL: https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos
- Date: change log, September 14, 2022
- "People generally remain many feet away from their stationary TV — often 8 feet or more."
- "Embrace the tvOS focus system … helping them know what to do and where they are at all times."
- Use it for: designing for a 10-foot viewing distance, with focus as the "you are here" cue.

---

## 3. Material Design 3 (S: m3.material.io not reachable from this session)

- Navigation drawer guidelines: https://m3.material.io/components/navigation-drawer/guidelines
  - (S, paraphrase) Use it for five or more primary destinations or more than one level of hierarchy. Labels should be concise and not clipped.
- Navigation rail guidelines: https://m3.material.io/components/navigation-rail/guidelines
  - (S, paraphrase) 3 to 7 items, each with a short label. Do not truncate labels or shrink the type to make them fit.
- UX writing best practices: https://m3.material.io/foundations/content-design/style-guide/ux-writing-best-practices
  - (S, paraphrase) Sentence case for titles, labels and menu items. Spell words out instead of abbreviating.
- Use them for: short, untruncated labels on every navigation item; no abbreviations.

---

## 4. Nielsen Norman Group (S: www.nngroup.com not reachable from this session)

- Low Findability and Discoverability: Four Testing Methods to Identify the Causes: https://www.nngroup.com/articles/navigation-ia-tests/
  - (S, paraphrase) Findability means people can find content they assume exists. Discoverability means people come across content they did not know about. Use IA research and UI research to tell which one is the cause.
  - Use it for: measuring findability (known settings) apart from discoverability (new features).
- Tree Testing: Evaluate Menu Labels and Categories: https://www.nngroup.com/articles/tree-testing/
  - (S, paraphrase) A quantitative test of how easily people find items in a hierarchy, with no visual design involved.
  - Use it for: testing the Arabic settings tree with tasks before building it.
- Card Sorting: Uncover Users' Mental Models: https://www.nngroup.com/articles/card-sorting-definition/
- Card Sorting vs. Tree Testing: https://www.nngroup.com/articles/card-sorting-tree-testing-differences/
  - (S, paraphrase) Card sorting generates the grouping. Tree testing checks it.
  - Use it for: deciding which settings belong together.
- 5 Tips for Avoiding Confusing Category Names: https://www.nngroup.com/articles/category-names-suck/
- 3 Common IA Mistakes (that Are All Due to Low Information Scent): https://www.nngroup.com/articles/3-ia-mistakes/
  - (S, paraphrase) Vague labels such as "Explore" or "Resources" don't tell people what is inside. Test whether people can predict a category's contents from its name.
  - Use it for: specific category names with strong information scent.
- Flat vs. Deep Website Hierarchies: https://www.nngroup.com/articles/flat-vs-deep-hierarchy/
  - (S, paraphrase) All else being equal, deep hierarchies are harder to use. Flattening too far makes labels broad and the top level long.
  - Use it for: a shallow tree, about two levels.
- Polyhierarchies Improve Findability for Ambiguous IA Categories: https://www.nngroup.com/articles/polyhierarchy/
  - (S, paraphrase) An item that fits more than one category can be listed under each one.
  - Use it for: putting an ambiguous setting (for example, the adhan volume) in both of the places people look for it.
- Onboarding Tutorials vs. Contextual Help: https://www.nngroup.com/articles/onboarding-tutorials/
  - (S, paraphrase) Introduce features at the moment they are relevant ("pull revelations"), not in a front-loaded tour. Feature callouts suit features that are really new.
  - Use it for: a "new" marker only on new items, shown in context and removed after the person has seen it.
- 7 Tips for Successful Customization: https://www.nngroup.com/articles/customization/
  - (S, paraphrase) Put customization controls near the content they affect, with clear names.
  - Use it for: shortcuts to a setting from the screen it changes.

(No NN/g article on search inside settings came up in search results.)

---

## 5. Android Settings app and W3C

- Android settings design guidelines (AOSP): https://source.android.com/docs/core/settings/settings-guidelines (S: source.android.com not reachable)
  - (S, paraphrase) Put frequently used settings at the top. More than 10 to 15 items on one screen can overwhelm people. Each search result shows its title, subtext and breadcrumb location.
  - Use it for: ordering by frequency, a cap per screen, search results that show the path to the setting.
- WCAG 2.2 Understanding 3.2.3 Consistent Navigation: https://www.w3.org/WAI/WCAG22/Understanding/consistent-navigation.html (S)
  - (S, paraphrase) Repeated navigation keeps the same relative order everywhere it appears.
- WCAG 2.2 Understanding 3.2.4 Consistent Identification: https://www.w3.org/WAI/WCAG22/Understanding/consistent-identification (S)
  - (S, paraphrase) The same function keeps the same name everywhere. Failure F31 is two labels for one function.
- Use them for: a stable category order, and one name per setting in the menu, in search, in help and on the phone page.

---

## 10 design rules for TV settings

1. **Keep the tree shallow: a top level and one level below, reached in the fewest screens.** Sources: https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv · https://www.nngroup.com/articles/flat-vs-deep-hierarchy/ (S)
2. **Keep the top level small and ordered by use: about 5 to 7 categories, the most used first, related ones together.** Sources: https://developer.android.com/design/ui/tv/guides/components/navigation-drawer · https://source.android.com/docs/core/settings/settings-guidelines (S)
3. **Use a two-pane layout with one job per axis: up and down moves through categories or rows, right goes deeper, and focus follows the hierarchy.** Sources: https://developer.android.com/design/ui/tv/guides/styles/layouts · https://developer.android.com/training/tv/get-started/navigation
4. **Name categories with short, specific, plain words from one shared glossary, never vague ones like "More" or "General", and test the names with a tree test.** Sources: https://developer.apple.com/design/human-interface-guidelines/writing · https://www.nngroup.com/articles/category-names-suck/ (S) · https://www.nngroup.com/articles/tree-testing/ (S)
5. **Give each setting one name and keep categories in the same order wherever they appear (menu, search, help, phone page).** Sources: https://www.w3.org/WAI/WCAG22/Understanding/consistent-identification (S) · https://www.w3.org/WAI/WCAG22/Understanding/consistent-navigation.html (S) · https://developer.apple.com/design/human-interface-guidelines/tab-bars
6. **Make each row one focus target with its current value visible (subtitle, switch or radio icon); no buttons inside rows and no colour-only state.** Sources: https://developer.android.com/design/ui/tv/guides/components/lists · https://developer.apple.com/design/human-interface-guidelines/focus-and-selection
7. **Keep focus predictable: something is always focused, there are no jumps the person did not cause, Back goes to the parent with focus on the item they came from, and the last place is restored.** Sources: https://developer.android.com/training/tv/get-started/navigation · https://developer.apple.com/design/human-interface-guidelines/remotes · https://developer.apple.com/design/human-interface-guidelines/focus-and-selection
8. **Offer fewer settings with good defaults, put task options where the task happens, and list a setting that fits two places in both.** Sources: https://developer.apple.com/design/human-interface-guidelines/settings · https://www.nngroup.com/articles/polyhierarchy/ (S) · https://www.nngroup.com/articles/customization/ (S)
9. **Make text readable at 10 feet and keep content inside the overscan-safe margins: large non-thin type, labels that don't wrap, about 5% margins.** Sources: https://developer.android.com/design/ui/tv/guides/styles/typography · https://developer.android.com/design/ui/tv/guides/styles/layouts · https://developer.apple.com/design/human-interface-guidelines/layout
10. **Use badges and "new" markers rarely: only on new items, shown in context, removed once seen; and if settings get search, make it one entry point with suggestions and results that show their path.** Sources: https://developer.android.com/design/ui/tv/guides/components/navigation-drawer · https://developer.apple.com/design/human-interface-guidelines/tab-bars · https://www.nngroup.com/articles/onboarding-tutorials/ (S) · https://developer.apple.com/design/human-interface-guidelines/searching · https://source.android.com/docs/core/settings/settings-guidelines (S)
