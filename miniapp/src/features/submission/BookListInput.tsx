type Props = { value: string; onChange: (value: string) => void };

export function BookListInput({ value, onChange }: Props) {
  return <section className="input-section">
    <div className="section-heading"><label htmlFor="book-list">Твой список</label><span>10 книг</span></div>
    <textarea id="book-list" value={value} onChange={(event) => onChange(event.target.value)}
      placeholder={"Вставь сюда список из заметок...\n\n1. Мастер и Маргарита\n2. 1984\n3. Цветы для Элджернона"} rows={8} />
    <div className="input-hint">Каждую книгу указывай с новой строки.</div>
  </section>;
}
