import { useEffect, useState } from "react";
import { fetchBookCatalog, type CatalogBook } from "./catalogApi";

interface Props {
  pollRoundId: number;
}

function BookCard({ book }: { book: CatalogBook }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <article className="catalog-book">
      <div className="catalog-book-main">
        {book.coverUrl ? (
          <img
            className="catalog-cover"
            src={book.coverUrl}
            alt={`Обложка: ${book.title}`}
          />
        ) : (
          <div className="catalog-cover-placeholder">📚</div>
        )}

        <div className="catalog-book-info">
          <h2>{book.title}</h2>

          {book.author && (
            <p className="catalog-author">{book.author}</p>
          )}

          {book.pageCount && (
            <span className="catalog-pages">
              📖 {book.pageCount} стр.
            </span>
          )}
        </div>
      </div>

      {book.description && (
        <>
          <button
            className="catalog-expand"
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? "Скрыть описание ↑" : "Читать описание ↓"}
          </button>

          {expanded && (
            <p className="catalog-description">
              {book.description}
            </p>
          )}
        </>
      )}
    </article>
  );
}

export function BookCatalog({ pollRoundId }: Props) {
  const [books, setBooks] = useState<CatalogBook[]>([]);
  const [roundNumber, setRoundNumber] = useState<number>(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchBookCatalog(pollRoundId)
      .then((data) => {
        setBooks(data.books);
        setRoundNumber(data.roundNumber);
      })
      .catch((err: Error) => {
        setError(err.message);
      })
      .finally(() => setLoading(false));
  }, [pollRoundId]);

  return (
    <main className="app catalog-app">
      <header className="header">
        <div className="logo">BOOK CLUB</div>
        <div className="month">BOOK CATALOG</div>
      </header>

      <section className="intro">
        <div className="eyebrow">
          КНИЖНЫЙ КЛУБ · ГОЛОСОВАНИЕ
        </div>
        <h1>Книги<br />на выбор</h1>
        <p>
          Знакомься с книгами, которые участвуют в голосовании.
        </p>
      </section>

      <div className="catalog-round">
        Раунд {roundNumber}
      </div>

      {loading && (
        <div className="catalog-state">
          Загружаем книги…
        </div>
      )}

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {!loading && !error && books.length === 0 && (
        <div className="catalog-state">
          В этом голосовании пока нет книг.
        </div>
      )}

      {!loading && !error && (
        <div className="catalog-list">
          {books.map((book, index) => (
            <BookCard key={`${index}-${book.title}`} book={book} />
          ))}
        </div>
      )}

      <footer>BOOK CLUB · READING TOGETHER</footer>
    </main>
  );
}