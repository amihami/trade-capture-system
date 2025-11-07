import React from "react";
import { useSearchParams } from "react-router-dom";
import Layout from "../components/Layout";
import { HomeContent } from "../components/HomeContent";
import { TradeBlotterModal } from "../modal/TradeBlotterModal";
import { TradeActionsModal } from "../modal/TradeActionsModal";
import staticStore from "../stores/staticStore";
import LoadingSpinner from "../components/LoadingSpinner";

const TraderSales = () => {
  const [searchParams] = useSearchParams();
  const view = searchParams.get("view") || "default";

  const [ready, setReady] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    // hydrate reference data for dropdowns (books, counterparties, users, etc.)
    const hydrate = async () => {
      try {
        if (typeof staticStore.ensureLoaded === "function") {
          await staticStore.ensureLoaded();
        } else {
          // Fallback to the full fetch if hydration does not work
          await staticStore.fetchAllStaticValues();
        }
      } catch (e: any) {
        setError("Failed to load reference data. Please refresh.");
        console.error("Static data hydration failed", e);
      } finally {
        setReady(true);
      }
    };
    hydrate();
  }, []);

  return (
    <div>
      <Layout>
        {!ready ? (
          <div className="flex items-center justify-center py-12">
            <LoadingSpinner />
            <span className="ml-3 text-sm text-gray-600">
              Loading reference data…
            </span>
          </div>
        ) : error ? (
          <div className="p-4 mx-4 my-6 rounded bg-red-50 text-red-700 text-sm">
            {error}
          </div>
        ) : (
          <>
            {view === "default" && <HomeContent />}
            {view === "actions" && <TradeActionsModal />}
            {view === "history" && <TradeBlotterModal />}
          </>
        )}
      </Layout>
    </div>
  );
};

export default TraderSales;
