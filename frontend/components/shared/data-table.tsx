"use client";

import { useState, useMemo, useCallback } from "react";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  MoreHorizontal,
  Search,
  Bot,
  Columns3,
  ChevronLeft,
  ChevronRight,
  PackageOpen,
} from "lucide-react";

// ============ Types ============

export type SortDirection = "asc" | "desc" | null;

export interface ColumnDef<T> {
  id: string;
  header: string;
  accessorKey?: keyof T;
  accessorFn?: (row: T) => React.ReactNode;
  sortable?: boolean;
  sortFn?: (a: T, b: T) => number;
  cell?: (row: T) => React.ReactNode;
  headerClassName?: string;
  cellClassName?: string;
  visible?: boolean;
}

export interface BulkAction {
  label: string;
  icon?: React.ReactNode;
  onClick: (selectedIds: string[]) => void;
  variant?: "default" | "destructive";
}

export interface RowAction<T> {
  label: string;
  icon?: React.ReactNode;
  onClick: (row: T) => void;
  variant?: "default" | "destructive";
}

export interface DataTableProps<T extends { id: string }> {
  data: T[];
  columns: ColumnDef<T>[];
  searchPlaceholder?: string;
  searchKey?: keyof T;
  filterSlots?: React.ReactNode;
  selectable?: boolean;
  bulkActions?: BulkAction[];
  primaryAction?: RowAction<T>;
  rowActions?: RowAction<T>[];
  showAgentAction?: boolean;
  onAgentAction?: (row: T) => void;
  onRowClick?: (row: T) => void;
  pageSize?: number;
  loading?: boolean;
  emptyState?: {
    icon?: React.ReactNode;
    title: string;
    description?: string;
    action?: React.ReactNode;
  };
  className?: string;
}

// ============ Component ============

export function DataTable<T extends { id: string }>({
  data,
  columns,
  searchPlaceholder = "Search...",
  searchKey,
  filterSlots,
  selectable = false,
  bulkActions = [],
  primaryAction,
  rowActions = [],
  showAgentAction = false,
  onAgentAction,
  onRowClick,
  pageSize = 10,
  loading = false,
  emptyState,
  className,
}: DataTableProps<T>) {
  const [search, setSearch] = useState("");
  const [sortColumn, setSortColumn] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [currentPage, setCurrentPage] = useState(0);
  const [hiddenColumns, setHiddenColumns] = useState<Set<string>>(new Set());

  // Visible columns
  const visibleColumns = useMemo(
    () => columns.filter((col) => !hiddenColumns.has(col.id)),
    [columns, hiddenColumns]
  );

  // Filtered data
  const filteredData = useMemo(() => {
    if (!search || !searchKey) return data;
    const term = search.toLowerCase();
    return data.filter((row) => {
      const val = row[searchKey];
      return String(val).toLowerCase().includes(term);
    });
  }, [data, search, searchKey]);

  // Sorted data
  const sortedData = useMemo(() => {
    if (!sortColumn || !sortDirection) return filteredData;
    const col = columns.find((c) => c.id === sortColumn);
    if (!col) return filteredData;

    return [...filteredData].sort((a, b) => {
      let result = 0;
      if (col.sortFn) {
        result = col.sortFn(a, b);
      } else if (col.accessorKey) {
        const aVal = String(a[col.accessorKey] ?? "");
        const bVal = String(b[col.accessorKey] ?? "");
        result = aVal.localeCompare(bVal);
      }
      return sortDirection === "desc" ? -result : result;
    });
  }, [filteredData, sortColumn, sortDirection, columns]);

  // Paginated data
  const totalPages = Math.max(1, Math.ceil(sortedData.length / pageSize));
  const paginatedData = useMemo(
    () =>
      sortedData.slice(currentPage * pageSize, (currentPage + 1) * pageSize),
    [sortedData, currentPage, pageSize]
  );

  // Selection helpers
  const allSelected =
    paginatedData.length > 0 &&
    paginatedData.every((r) => selectedIds.has(r.id));
  const someSelected = paginatedData.some((r) => selectedIds.has(r.id));

  const toggleAll = useCallback(() => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allSelected) {
        paginatedData.forEach((r) => next.delete(r.id));
      } else {
        paginatedData.forEach((r) => next.add(r.id));
      }
      return next;
    });
  }, [allSelected, paginatedData]);

  const toggleRow = useCallback((id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const handleSort = useCallback(
    (colId: string) => {
      setSortColumn((prev) => {
        if (prev !== colId) {
          setSortDirection("asc");
          return colId;
        }
        setSortDirection((d) => {
          if (d === "asc") return "desc";
          if (d === "desc") return null;
          return "asc";
        });
        if (sortDirection === "desc") return null;
        return colId;
      });
    },
    [sortDirection]
  );

  const toggleColumnVisibility = useCallback((colId: string) => {
    setHiddenColumns((prev) => {
      const next = new Set(prev);
      if (next.has(colId)) next.delete(colId);
      else next.add(colId);
      return next;
    });
  }, []);

  const getCellValue = useCallback(
    (row: T, col: ColumnDef<T>) => {
      if (col.cell) return col.cell(row);
      if (col.accessorFn) return col.accessorFn(row);
      if (col.accessorKey) return String(row[col.accessorKey] ?? "");
      return "";
    },
    []
  );

  const hasActions = primaryAction || rowActions.length > 0 || showAgentAction;

  // Suppress lint warning for someSelected (used for future indeterminate state)
  void someSelected;

  // ============ Loading state ============
  if (loading) {
    return (
      <div className={cn("space-y-3", className)}>
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-64" />
          <Skeleton className="h-9 w-24" />
        </div>
        <div className="rounded-lg border">
          <div className="border-b px-4 py-3">
            <div className="flex gap-6">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-4 w-24" />
              ))}
            </div>
          </div>
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="flex gap-6 border-b px-4 py-3 last:border-0"
            >
              {Array.from({ length: 4 }).map((_, j) => (
                <Skeleton key={j} className="h-4 w-24" />
              ))}
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={cn("space-y-3", className)}>
      {/* ============ Toolbar ============ */}
      <div className="flex flex-wrap items-center gap-2">
        {searchKey && (
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={searchPlaceholder}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(0);
              }}
              className="h-9 w-64 pl-8 text-sm"
            />
          </div>
        )}
        {filterSlots}
        <div className="flex-1" />
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline" size="sm" className="gap-1.5">
              <Columns3 className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Columns</span>
            </Button>
          </PopoverTrigger>
          <PopoverContent align="end" className="w-48 p-2">
            {columns.map((col) => (
              <label
                key={col.id}
                className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-accent"
              >
                <Checkbox
                  checked={!hiddenColumns.has(col.id)}
                  onCheckedChange={() => toggleColumnVisibility(col.id)}
                />
                {col.header}
              </label>
            ))}
          </PopoverContent>
        </Popover>
      </div>

      {/* ============ Bulk Actions Bar ============ */}
      {selectable && selectedIds.size > 0 && bulkActions.length > 0 && (
        <div className="flex items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-4 py-2">
          <span className="font-mono text-xs font-medium text-primary">
            {selectedIds.size} selected
          </span>
          <div className="mx-2 h-4 w-px bg-border" />
          {bulkActions.map((action, i) => (
            <Button
              key={i}
              variant={
                action.variant === "destructive" ? "destructive" : "ghost"
              }
              size="sm"
              onClick={() => action.onClick(Array.from(selectedIds))}
              className="gap-1.5"
            >
              {action.icon}
              {action.label}
            </Button>
          ))}
          <div className="flex-1" />
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSelectedIds(new Set())}
            className="text-xs text-muted-foreground"
          >
            Clear
          </Button>
        </div>
      )}

      {/* ============ Table ============ */}
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              {selectable && (
                <TableHead className="w-10 px-3">
                  <Checkbox
                    checked={allSelected}
                    onCheckedChange={toggleAll}
                    aria-label="Select all"
                  />
                </TableHead>
              )}
              {visibleColumns.map((col) => (
                <TableHead
                  key={col.id}
                  className={cn(
                    "text-[11px] font-semibold uppercase tracking-wider",
                    col.sortable !== false && "cursor-pointer select-none",
                    col.headerClassName
                  )}
                  onClick={() =>
                    col.sortable !== false && handleSort(col.id)
                  }
                >
                  <div className="flex items-center gap-1">
                    {col.header}
                    {col.sortable !== false && (
                      <span className="text-muted-foreground/50">
                        {sortColumn === col.id &&
                        sortDirection === "asc" ? (
                          <ArrowUp className="h-3 w-3" />
                        ) : sortColumn === col.id &&
                          sortDirection === "desc" ? (
                          <ArrowDown className="h-3 w-3" />
                        ) : (
                          <ArrowUpDown className="h-3 w-3" />
                        )}
                      </span>
                    )}
                  </div>
                </TableHead>
              ))}
              {hasActions && (
                <TableHead className="w-12 text-right text-[11px] font-semibold uppercase tracking-wider">
                  Actions
                </TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginatedData.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={
                    visibleColumns.length +
                    (selectable ? 1 : 0) +
                    (hasActions ? 1 : 0)
                  }
                  className="h-48"
                >
                  <div className="flex flex-col items-center justify-center gap-2 text-center">
                    {emptyState?.icon ?? (
                      <PackageOpen className="h-8 w-8 text-muted-foreground/40" />
                    )}
                    <p className="text-sm font-medium text-foreground/70">
                      {emptyState?.title ?? "No data"}
                    </p>
                    {emptyState?.description && (
                      <p className="text-xs text-muted-foreground">
                        {emptyState.description}
                      </p>
                    )}
                    {emptyState?.action}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              paginatedData.map((row) => (
                <TableRow
                  key={row.id}
                  className={cn(
                    selectedIds.has(row.id) && "bg-primary/5",
                    onRowClick && "cursor-pointer"
                  )}
                  onClick={() => onRowClick?.(row)}
                >
                  {selectable && (
                    <TableCell
                      className="w-10 px-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Checkbox
                        checked={selectedIds.has(row.id)}
                        onCheckedChange={() => toggleRow(row.id)}
                        aria-label={`Select row ${row.id}`}
                      />
                    </TableCell>
                  )}
                  {visibleColumns.map((col) => (
                    <TableCell
                      key={col.id}
                      className={cn("text-sm", col.cellClassName)}
                    >
                      {getCellValue(row, col)}
                    </TableCell>
                  ))}
                  {hasActions && (
                    <TableCell
                      className="w-12 text-right"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-0.5">
                        {primaryAction && (
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            onClick={() => primaryAction.onClick(row)}
                          >
                            {primaryAction.icon ?? primaryAction.label}
                          </Button>
                        )}
                        {showAgentAction && (
                          <Button
                            variant="ghost"
                            size="icon-xs"
                            onClick={() => onAgentAction?.(row)}
                            className="text-primary"
                          >
                            <Bot className="h-3 w-3" />
                          </Button>
                        )}
                        {rowActions.length > 0 && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon-xs">
                                <MoreHorizontal className="h-3.5 w-3.5" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent
                              align="end"
                              className="w-44"
                            >
                              {rowActions.map((action, i) => (
                                <DropdownMenuItem
                                  key={i}
                                  onClick={() => action.onClick(row)}
                                  className={cn(
                                    "gap-2 text-sm",
                                    action.variant === "destructive" &&
                                      "text-destructive"
                                  )}
                                >
                                  {action.icon}
                                  {action.label}
                                </DropdownMenuItem>
                              ))}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </div>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* ============ Pagination ============ */}
      {sortedData.length > pageSize && (
        <div className="flex items-center justify-between px-1">
          <p className="font-mono text-xs text-muted-foreground">
            {currentPage * pageSize + 1}&ndash;
            {Math.min((currentPage + 1) * pageSize, sortedData.length)} of{" "}
            {sortedData.length}
          </p>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="icon-xs"
              disabled={currentPage === 0}
              onClick={() => setCurrentPage((p) => p - 1)}
            >
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <span className="px-2 font-mono text-xs text-muted-foreground">
              {currentPage + 1} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="icon-xs"
              disabled={currentPage >= totalPages - 1}
              onClick={() => setCurrentPage((p) => p + 1)}
            >
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
