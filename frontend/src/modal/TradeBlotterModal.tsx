import React from "react";
import { observer } from "mobx-react-lite";
import AGGridTable from "../components/AGGridTable";
import {
  fetchTrades,
  searchTradesBySettlement,
  fetchTradeById,
} from "../utils/api";
import { getColDefFromResult, getRowDataFromData } from "../utils/agGridUtils";
import { useQuery } from "@tanstack/react-query";
import { Trade } from "../utils/tradeTypes";
import SingleTradeModal from "./SingleTradeModal";

export const TradeBlotterModal: React.FC = observer(() => {
  const [trades, setTrades] = React.useState<Trade[]>([]);
  const [query, setQuery] = React.useState<string>("");
  const [isSearching, setIsSearching] = React.useState(false);

  // modal state
  const [editOpen, setEditOpen] = React.useState(false);
  const [editingTrade, setEditingTrade] = React.useState<Trade | undefined>(
    undefined
  );

  // default data feed (auto refresh)
  const { data, isSuccess, isFetching, refetch } = useQuery({
    queryKey: ["trades"],
    queryFn: async () => {
      const res = await fetchTrades();
      return res.data;
    },
    refetchInterval: 30000,
    refetchIntervalInBackground: true,
  });

  // load default grid data
  React.useEffect(() => {
    if (isSuccess && data && !isSearching && query.trim() === "") {
      setTrades(data);
    }
  }, [isSuccess, data, isSearching, query]);

  // debounce search (300ms)
  React.useEffect(() => {
    const q = query.trim();
    if (q.length < 3) {
      setIsSearching(false);
      if (isSuccess && data) setTrades(data);
      return;
    }
    setIsSearching(true);
    const handle = setTimeout(async () => {
      try {
        const res = await searchTradesBySettlement(q);
        setTrades(res.data || []);
      } catch {
        // keep current data on error
      } finally {
        setIsSearching(false);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [query, isSuccess, data]);

  // build columns from data shape
  const baseColDefs = getColDefFromResult(trades);

  // enhance settlementInstructions column + add Actions column
  const columnDefs = [
    ...baseColDefs.map((col: any) => {
      if (col.field === "settlementInstructions") {
        return {
          ...col,
          headerName: "Settlement Instructions",
          tooltipField: "settlementInstructions",
          flex: 2,
          minWidth: 300,
          wrapText: true,
          autoHeight: true,
          cellRenderer: (p: any) => {
            const v = p.value ?? "";
            return (
              <div
                title={v}
                style={{ whiteSpace: "pre-wrap", lineHeight: "1.2" }}
              >
                {v}
              </div>
            );
          },
        };
      }
      return col;
    }),
    {
      headerName: "Actions",
      field: "__actions",
      pinned: "right",
      minWidth: 130,
      maxWidth: 150,
      cellRenderer: (p: any) => {
        const tradeId = p.data?.tradeId;
        return (
          <div className="flex gap-2">
            <button
              className="px-2 py-1 text-xs rounded bg-blue-600 text-white hover:bg-blue-700"
              title="Edit trade"
              onClick={async () => {
                if (!tradeId) return;
                try {
                  const res = await fetchTradeById(tradeId);
                  setEditingTrade(res.data);
                  setEditOpen(true);
                } catch (e) {
                  console.error("Failed to fetch trade", e);
                }
              }}
            >
              Edit
            </button>
          </div>
        );
      },
    },
  ];

  const rowData = getRowDataFromData(trades);

  return (
    <div
      className={
        "h-fit w-full flex flex-col min-h-full min-w-full justify-start"
      }
    >
      {/* Quick search bar */}
      <div className="p-2 flex items-center gap-2">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search settlement instructions… (min 3 chars)"
          className="border rounded px-3 py-2 text-sm w-[28rem]"
        />
        <span className="text-xs text-gray-500">
          {isFetching ? "Refreshing…" : isSearching ? "Searching…" : ""}
        </span>
      </div>

      <div>
        <AGGridTable
          columnDefs={columnDefs}
          rowData={rowData}
          onSelectionChanged={() => {}}
          rowSelection={"single"}
        />
      </div>

      {/* Edit modal with full-width white background */}
      {editOpen && editingTrade && (
        <div className="fixed inset-0 z-50 overflow-auto bg-black/40 flex items-center justify-center">
          <div className="mx-auto my-8 w-[95%] max-w-[1600px]">
            <div className="bg-white rounded-2xl shadow-2xl p-8">
              <SingleTradeModal
                mode="edit"
                isOpen={editOpen}
                trade={editingTrade}
                onClear={() => {
                  setEditOpen(false);
                  setEditingTrade(undefined);
                  refetch(); // refresh blotter after save
                }}
              />
              <div className="mt-4 flex justify-end">
                <button
                  className="px-3 py-1 text-sm rounded bg-gray-200 hover:bg-gray-300"
                  onClick={() => {
                    setEditOpen(false);
                    setEditingTrade(undefined);
                  }}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
});
