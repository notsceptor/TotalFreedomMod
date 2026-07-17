# TotalFreedom Developer Contribution Guidelines

These guidelines apply to contributions to any development repository in the
`tfreedomorg` organization. Follow them to maximize compatibility, maintain a
consistent workflow, and support organized collaboration.

Source: [TotalFreedom Developer Contribution Guidelines (12 July 2026)](https://forum.tfreedom.org/t/guideline-totalfreedom-developer-contribution-guidelines-12-july-2026/157)

## Development Workflow

- Keep commits and pull requests small. Do not combine a large feature or a
  broad redesign into one pull request.
- Scoped branches merging into `main` or `devel` are exempt from the pull
  request size rule. Individual contributions to a scoped branch must still be
  small and focused.
- AI may facilitate the development process, but it must not generate the
  implementation. The developer must perform the actionable coding step.
- Exploit patches may be marked as expedited. Expedited patches are merged
  automatically into the development branch.
- Follow the project's formatting rules consistently across the entire codebase.
- Give complex or non-intuitive code explicit identifier names. Add Javadoc, or
  at minimum comments on the most convoluted sections, explaining what the code
  does.

The project may introduce a ticketing system in the future. When available,
scoped work should ideally flow through a Kanban board with swimlanes organized
by scope and importance. These guidelines will be updated when that policy is
defined.

See also the [TotalFreedom Development Roadmap](https://forum.tfreedom.org/t/development-roadmap-12-july-2026/168).

## Code Formatting

### General Style

- Use Allman formatting for objects and their contents.
- Make values `final` or effectively final wherever possible.
- Prefer `StringBuilder` or `String.format()` over string concatenation with
  `+`.
- Remove unused imports.
- Prefer dependency injection. Direct use of the static singleton in the main
  plugin class is deprecated and will eventually be replaced by a `Supplier`.
- Do not add overloads solely to call other overloads. Adjust the original
  method or use a different design.
- For a single-line `if` body, place the statement on the following line and do
  not add braces.
- Prefer lambdas and streams. Use a `for` loop only when no suitable alternative
  exists.
- Place `else`, `catch`, and `finally` on a new line after the preceding closing
  brace.
- Keep braces for empty blocks on the same line, for example:
  `catch (Exception ignored) {}`.
- Use modern arrow-style switch cases (`->`), not legacy colon-style cases.
- Use `Optional` for values that may otherwise return `null`. New code should
  move toward strict non-null architecture.
- Do not create inner classes unless they are private static utility classes or
  modern Java `record` types used only for data transfer.

### Naming Conventions

| Element | Convention |
| --- | --- |
| Non-static variables | `camelCase` |
| Methods | `camelCase` |
| Static variables | `SCREAMING_SNAKE_CASE` |
| Classes and class files | `PascalCase` |
| Configuration variables | `snake_case` |
| Plugin command classes | `Command_<identifier>` |

### Streams

- Put every intermediate and terminal stream operation on its own line.
- Align stream operations by the dot (`.`).
- Prefer method references such as `String::isEmpty` over explicit lambdas such
  as `value -> value.isEmpty()` whenever possible.

### Class Content Ordering

Order class members as follows:

1. Static fields (`SCREAMING_SNAKE_CASE`), private first and public last.
2. Instance fields (`camelCase`), private first and public last.
3. Constructors.
4. Public methods and plugin commands.
5. Private helper methods.
