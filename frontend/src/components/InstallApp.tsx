"use client";

import { useEffect, useState } from "react";

/** "Install app" button: uses the browser's install prompt, or explains how on iPhone/Safari. */
export default function InstallApp() {
  const [prompt, setPrompt] = useState<any>(null);
  const [installed, setInstalled] = useState(false);
  const [help, setHelp] = useState(false);

  useEffect(() => {
    if ("serviceWorker" in navigator) navigator.serviceWorker.register("/sw.js").catch(() => undefined);
    if (window.matchMedia("(display-mode: standalone)").matches || (navigator as any).standalone || (window as any).NomadNative) setInstalled(true);
    const onPrompt = (e: Event) => {
      e.preventDefault();
      setPrompt(e);
    };
    const onInstalled = () => setInstalled(true);
    window.addEventListener("beforeinstallprompt", onPrompt);
    window.addEventListener("appinstalled", onInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", onPrompt);
      window.removeEventListener("appinstalled", onInstalled);
    };
  }, []);

  if (installed) return null;

  const click = async () => {
    if (prompt) {
      prompt.prompt();
      await prompt.userChoice.catch(() => undefined);
      setPrompt(null);
    } else {
      setHelp((v) => !v);
    }
  };

  const ios = typeof navigator !== "undefined" && /iphone|ipad|ipod/i.test(navigator.userAgent);

  return (
    <div className="space-y-2">
      <button type="button" onClick={click} className="btn btn-secondary btn-sm w-full">Install app</button>
      {help && (
        <p role="note" className="text-xs text-slate-400">
          {ios
            ? "In Safari, tap Share, then “Add to Home Screen”."
            : "Use your browser menu: “Install NOMAD” or “Add to Home screen”. (Chrome and Edge show an install icon in the address bar.)"}
        </p>
      )}
    </div>
  );
}
