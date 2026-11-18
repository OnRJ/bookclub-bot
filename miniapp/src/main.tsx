
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import {
  init,
  miniApp,
  viewport,
  retrieveLaunchParams,
} from "@tma.js/sdk";

import "./index.css";
import App from "./App.tsx";

const root = createRoot(document.getElementById("root")!);

root.render(
  <StrictMode>
    <App />
  </StrictMode>
);

async function initTelegram() {
  try {
    retrieveLaunchParams();
  } catch {
    console.info("App opened outside Telegram");
    return;
  }

  try {
    init();
    miniApp.mount();
    miniApp.bindCssVars();
    miniApp.ready();

    await viewport.mount({ timeout: 5000 });

    viewport.bindCssVars();
    viewport.expand();
  } catch (error) {
    console.error("Telegram initialization error:", error);
  }
}

void initTelegram();
