import "@testing-library/jest-dom/vitest";

Object.defineProperty(window, "Telegram", {
  writable: true,
  value: { WebApp: { initData: "test-init-data" } },
});
