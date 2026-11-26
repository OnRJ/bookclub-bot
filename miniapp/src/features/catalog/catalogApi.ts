const API_URL = import.meta.env.VITE_API_BASE_URL || "";

export interface CatalogBook {
  title: string;
  author: string | null;
  description: string | null;
  pageCount: number | null;
}

export interface BookCatalogResponse {
  pollRoundId: number;
  roundNumber: number;
  books: CatalogBook[];
}

export async function fetchBookCatalog(
  pollRoundId: number
): Promise<BookCatalogResponse> {
  const initData = window.Telegram?.WebApp?.initData;

  if (!initData) {
    throw new Error("Открой каталог через Telegram.");
  }

  const response = await fetch(
    `${API_URL}/api/book-catalog/${pollRoundId}?initData=${encodeURIComponent(initData)}`
  );

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new Error(data?.message ?? "Не удалось загрузить каталог.");
  }

  return response.json();
}
