
export function parseBookList(input: string): string[] {
  return input
    .split(/\r?\n/)
    .map((line) => line.trim())
    // Убираем нумерацию и маркеры списков.
    .map((line) =>
      line.replace(/^(?:\d+\s*[.)]|[-*•‣▪])\s*/, "").trim()
    )
    // Убираем пустые строки.
    .filter(Boolean);
}

export function normalizeBookTitle(title: string): string {
  return title
    .normalize("NFC")
    .toLocaleLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}
