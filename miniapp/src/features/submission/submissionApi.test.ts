import { beforeEach, describe, expect, it, vi } from "vitest";
import { submitBooks } from "./submissionApi";

describe("submitBooks", () => {
  beforeEach(() => {
    Object.defineProperty(window, "Telegram", { writable: true, value: { WebApp: { initData: "init" } } });
  });

  it("rejects when opened outside Telegram", async () => {
    window.Telegram!.WebApp!.initData = "";
    await expect(submitBooks(["Book"])).rejects.toThrow("Открой приложение через Telegram");
  });

  it("returns a successful response", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, json: async () => ({ message: "ok" }) }));
    await expect(submitBooks(["Book"])).resolves.toEqual({ message: "ok" });
  });

  it("uses a fallback for malformed errors", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, json: async () => ({}) }));
    await expect(submitBooks(["Book"])).rejects.toThrow("Не удалось отправить список книг");
  });
});
