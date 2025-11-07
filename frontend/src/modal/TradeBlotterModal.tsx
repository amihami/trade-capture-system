import React from "react";
import {observer} from "mobx-react-lite";
import AGGridTable from "../components/AGGridTable";
import {fetchTrades, searchTradesBySettlement} from "../utils/api";
import {getColDefFromResult, getRowDataFromData} from "../utils/agGridUtils";
import { useQuery } from '@tanstack/react-query';
import {Trade} from "../utils/tradeTypes";

export const TradeBlotterModal: React.FC = observer(() => {
  const [trades, setTrades] = React.useState<Trade[]>([]);
  const [query, setQuery] = React.useState<string>("");     
  const [isSearching, setIsSearching] = React.useState(false);

  // default data feed (auto refresh)
  const {data, isSuccess, isFetching} = useQuery({
    queryKey: ['trades'],
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
      // show default data again
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
        // swallow errors for quick UX; keep current trades
      } finally {
        setIsSearching(false);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [query, isSuccess, data]);

  // build columns from data shape
  const baseColDefs = getColDefFromResult(trades);

  // enhance settlementInstructions column: wider, tooltip, multiline
  const columnDefs = baseColDefs.map((col: any) => {
    if (col.field === 'settlementInstructions') {
      return {
        ...col,
        headerName: 'Settlement Instructions',
        tooltipField: 'settlementInstructions',
        flex: 2,
        minWidth: 300,
        wrapText: true,         
        autoHeight: true,      
        cellRenderer: (p: any) => {
          const v = p.value ?? '';
          return (
            <div title={v} style={{ whiteSpace: 'pre-wrap', lineHeight: '1.2' }}>
              {v}
            </div>
          );
        },
      };
    }
    return col;
  });

  const rowData = getRowDataFromData(trades);

  return (
    <div className={"h-fit w-full flex flex-col min-h-full min-w-full justify-start"}>
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
    </div>
  );
});
