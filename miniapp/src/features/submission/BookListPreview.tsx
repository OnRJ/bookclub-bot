import { MAX_BOOKS } from "./bookListValidator";
import type { BookListValidation } from "./bookListValidator";

type Props = { books: string[]; validation: BookListValidation; onUpdate: (index: number, value: string) => void; onRemove: (index: number) => void; onAdd: () => void };

export function BookListPreview({ books, validation, onUpdate, onRemove, onAdd }: Props) {
  return <section className="preview-section">
    <div className="section-heading"><label>Твои книги</label><span className={validation.isValid ? "count complete" : "count"}>{validation.count} / {MAX_BOOKS}</span></div>
    <div className="progress-track"><div className="progress-value" style={{ width: `${Math.min(validation.count / MAX_BOOKS, 1) * 100}%` }} /></div>
    {books.length === 0 ? <div className="empty-state">Здесь появятся твои книги</div> : <div className="book-list">
      {books.map((book, index) => <div className="book-item" key={index}>
        <span className="book-number">{index + 1}</span>
        <input value={book} onChange={(event) => onUpdate(index, event.target.value)} placeholder="Название книги" aria-label={`Книга ${index + 1}`} />
        <button type="button" className="remove-book" onClick={() => onRemove(index)} aria-label={`Удалить книгу ${index + 1}`} title="Удалить книгу">×</button>
        {validation.duplicateIndexes.includes(index) && <span className="duplicate-indicator">!</span>}
      </div>)}
    </div>}
    {books.length < MAX_BOOKS && <button type="button" className="add-book-button" onClick={onAdd}>+ Добавить книгу</button>}
    {validation.hasTooFewBooks && books.length > 0 && <p className="helper-message">Осталось добавить: {validation.remaining}</p>}
    {validation.hasTooManyBooks && <p className="error-message">В списке больше 10 книг. Удали лишние.</p>}
    {validation.duplicateIndexes.length > 0 && <p className="error-message">Есть повторяющиеся названия. Проверь отмеченные строки.</p>}
    {validation.hasEmptyBooks && <p className="error-message">Заполни пустые строки или удали их.</p>}
    {validation.isValid && <p className="success-message">Отлично! Все 10 книг готовы ❤️</p>}
  </section>;
}
