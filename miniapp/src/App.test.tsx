import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi, beforeEach } from "vitest";
import App from "./App";

const tenBooks = Array.from({ length: 10 }, (_, index) => `Book ${index + 1}`);

describe("App", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => cleanup());

  it("parses a list and shows books as read-only preview", () => {
    render(<App />);
    const textarea = screen.getByLabelText("Твой список");
    fireEvent.change(textarea, { target: { value: tenBooks.join("\n") } });

    expect(screen.getByText("10 / 10")).toBeInTheDocument();
    expect(screen.getByLabelText("Книга 1")).toHaveTextContent("Book 1");
    expect(screen.queryByRole("button", { name: /Удалить книгу/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Добавить книгу/ })).not.toBeInTheDocument();
  });

  it("submits valid books and shows success", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ message: "Отправлено", submissionId: 1, readingMonth: "2026-12", bookCount: 10 }),
    }));
    render(<App />);
    fireEvent.change(screen.getByLabelText("Твой список"), { target: { value: tenBooks.join("\n") } });
    fireEvent.click(screen.getAllByRole("button", { name: "Отправить список" })[0]);

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Отправлено"));
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("shows API errors", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: "Список уже отправлен" }),
    }));
    render(<App />);
    fireEvent.change(screen.getByLabelText("Твой список"), { target: { value: tenBooks.join("\n") } });
    fireEvent.click(screen.getAllByRole("button", { name: "Отправить список" })[0]);

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Список уже отправлен"));
  });
});
