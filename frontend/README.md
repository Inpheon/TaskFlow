# Frontend

## Struktura

- `pages/` zawiera komponenty przypisane do routes.
- `components/` zawiera wspolne elementy interfejsu.
- `api/` jest jedynym miejscem wykonywania requestow HTTP.
- `stores/` zawiera globalny stan Pinia.
- `types/` zawiera typy kontraktu API.
- `router/` zawiera routes i guardy dostepu.

## Zachowanie wspolnego stylu

- Nowy ekran powinien uzywac `AppShell`, `PageHeader` i `StatePanel`.
- Przyciski formularzy powinny uzywac `BaseButton`, a pola `FormField`.
- Ikony pochodza z `@lucide/vue`; przyciski ikonowe wymagaja `title` i `aria-label`.
- Kazdy ekran danych musi obslugiwac loading, empty i error state.
- Bledy backendu nalezy wyswietlac przez `ApiClientError`.
- Requesty nie powinny byc wykonywane bezposrednio w store lub komponencie, jezeli mozna dodac funkcje w `api/`.
- Stan lokalny ekranu pozostaje w komponencie. Pinia jest przeznaczona dla stanu wspoldzielonego.
- Wspolne kolory, odstepy, promienie i rozmiary kontrolek sa zdefiniowane jako zmienne `--color-*`, `--space-*`, `--radius-*` i `--control-*` w `style.css`.
- Widoczny tekst interfejsu pozostaje po angielsku.

## Komendy

```bash
npm install
npm run dev
npm run build
```
