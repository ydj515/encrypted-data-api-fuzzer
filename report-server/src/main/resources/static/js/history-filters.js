(function () {
  const form = document.getElementById("historyFilterForm");
  const resetBtn = document.getElementById("historyResetFilterBtn");
  const table = document.getElementById("historyRunsTable");
  const countEl = document.getElementById("historyRunCount");
  const emptyState = document.getElementById("historyEmptyState");
  const tableWrapper = table ? table.closest(".table-wrapper") : null;
  const bootstrapEl = document.getElementById("historyPageData");

  const apiInput = document.getElementById("historyApiFilterInput");
  const apiPanel = document.getElementById("historyApiAutocompletePanel");
  const sourceFilter = document.getElementById("historySourceFilter");
  const statusFilter = document.getElementById("historyStatusFilter");
  const methodFilter = document.getElementById("historyMethodFilter");
  const httpStatusFilter = document.getElementById("historyHttpStatusFilter");
  const caseNameInput = document.getElementById("historyCaseNameFilter");
  const caseNamePanel = document.getElementById("historyCaseNameAutocompletePanel");
  const endpointInput = document.getElementById("historyEndpointFilter");
  const endpointPanel = document.getElementById("historyEndpointAutocompletePanel");
  const fromInput = document.getElementById("historyFromFilter");
  const toInput = document.getElementById("historyToFilter");

  if (!form || !bootstrapEl || !apiInput || !apiPanel) {
    return;
  }

  let pageData = JSON.parse(bootstrapEl.textContent || "[]");
  let metadataComplete = bootstrapEl.dataset.metadataComplete === "true";
  let metadataLoadPromise = null;
  const metadataUrl = bootstrapEl.dataset.metadataUrl || "";
  const rows = table ? Array.from(table.querySelectorAll("tbody tr")) : [];
  const methodOrder = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"];
  const maxAutocompleteOptions = 8;

  let activeAutocomplete = null;
  let activeIndex = -1;
  let currentOptions = [];

  const escapeHtml = (value) =>
    value
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");

  const normalize = (value) => (value || "").trim().toLowerCase();

  const toTextList = (values) =>
    Array.isArray(values) ? values.filter((value) => typeof value === "string" && value.trim()) : [];

  const toStringList = (values) =>
    Array.isArray(values) ? values.filter((value) => value !== null && value !== undefined).map(String) : [];

  const createSearchableList = (values) => {
    const items = toTextList(values);
    return {
      values: items,
      normalized: items.map((value) => normalize(value)),
    };
  };

  const parseDateMs = (value, isEnd) => {
    if (!value) {
      return null;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    if (isEnd) {
      date.setSeconds(59, 999);
    }
    return date.getTime();
  };

  const uniqueSorted = (values, compareFn) =>
    Array.from(new Set(values.filter((value) => value))).sort(compareFn);

  const compareText = (left, right) => left.localeCompare(right);

  const compareMethods = (left, right) => {
    const leftIndex = methodOrder.indexOf(left);
    const rightIndex = methodOrder.indexOf(right);
    if (leftIndex >= 0 || rightIndex >= 0) {
      const normalizedLeftIndex = leftIndex >= 0 ? leftIndex : Number.MAX_SAFE_INTEGER;
      const normalizedRightIndex = rightIndex >= 0 ? rightIndex : Number.MAX_SAFE_INTEGER;
      if (normalizedLeftIndex !== normalizedRightIndex) {
        return normalizedLeftIndex - normalizedRightIndex;
      }
    }
    return left.localeCompare(right);
  };

  const emptyMetadata = () => ({
    apis: [],
    caseNames: [],
    endpoints: [],
    methods: [],
    httpStatuses: [],
  });

  const metadataByRunId = new Map();

  const normalizeMetadataRow = (row) => ({
    apis: toTextList(row.apis),
    caseNames: toTextList(row.caseNames),
    endpoints: toTextList(row.endpoints),
    methods: toTextList(row.httpMethods).map((value) => value.toUpperCase()),
    httpStatuses: toStringList(row.httpStatuses),
  });

  const applyPageData = (data) => {
    data
      .filter((row) => row && row.run && row.run.id)
      .forEach((row) => metadataByRunId.set(row.run.id, normalizeMetadataRow(row)));
  };

  applyPageData(pageData);

  const applyMetadataToState = (state) => {
    const metadata = metadataByRunId.get(state.runId) || emptyMetadata();
    const apis = createSearchableList(metadata.apis);
    const caseNames = createSearchableList(metadata.caseNames);
    const endpoints = createSearchableList(metadata.endpoints);

    state.apis = apis.values;
    state.apisNormalized = apis.normalized;
    state.caseNames = caseNames.values;
    state.caseNamesNormalized = caseNames.normalized;
    state.endpoints = endpoints.values;
    state.endpointsNormalized = endpoints.normalized;
    state.methods = metadata.methods;
    state.httpStatuses = metadata.httpStatuses;
  };

  const rowStates = rows.map((row) => {
    const runId = row.dataset.runId || "";
    const state = {
      row,
      runId,
      source: row.dataset.source || "",
      status: row.dataset.status || "",
      startedAtMs: row.dataset.startedAt ? parseDateMs(row.dataset.startedAt, false) : null,
      apis: [],
      apisNormalized: [],
      caseNames: [],
      caseNamesNormalized: [],
      endpoints: [],
      endpointsNormalized: [],
      methods: [],
      httpStatuses: [],
    };
    applyMetadataToState(state);
    return state;
  });

  const currentFilters = () => ({
    api: normalize(apiInput.value),
    source: sourceFilter ? sourceFilter.value : "",
    status: statusFilter ? statusFilter.value : "",
    method: methodFilter ? methodFilter.value : "",
    httpStatus: httpStatusFilter ? httpStatusFilter.value : "",
    caseName: normalize(caseNameInput ? caseNameInput.value : ""),
    endpoint: normalize(endpointInput ? endpointInput.value : ""),
    fromMs: parseDateMs(fromInput ? fromInput.value : "", false),
    toMs: parseDateMs(toInput ? toInput.value : "", true),
  });

  const listContains = (normalizedValues, query) => {
    if (!query) {
      return true;
    }
    return normalizedValues.some((value) => value.includes(query));
  };

  const matchesFilters = (state, excludeKey) => {
    const filters = currentFilters();

    if (excludeKey !== "source" && filters.source && state.source !== filters.source) {
      return false;
    }
    if (excludeKey !== "status" && filters.status && state.status !== filters.status) {
      return false;
    }
    if (excludeKey !== "api" && !listContains(state.apisNormalized, filters.api)) {
      return false;
    }
    if (excludeKey !== "method" && filters.method && !state.methods.includes(filters.method)) {
      return false;
    }
    if (
      excludeKey !== "httpStatus" &&
      filters.httpStatus &&
      !state.httpStatuses.includes(filters.httpStatus)
    ) {
      return false;
    }
    if (excludeKey !== "caseName" && !listContains(state.caseNamesNormalized, filters.caseName)) {
      return false;
    }
    if (excludeKey !== "endpoint" && !listContains(state.endpointsNormalized, filters.endpoint)) {
      return false;
    }
    if (excludeKey !== "from" && filters.fromMs != null) {
      if (state.startedAtMs == null || state.startedAtMs < filters.fromMs) {
        return false;
      }
    }
    if (excludeKey !== "to" && filters.toMs != null) {
      if (state.startedAtMs == null || state.startedAtMs > filters.toMs) {
        return false;
      }
    }
    return true;
  };

  const visibleStates = (excludeKey) => rowStates.filter((state) => matchesFilters(state, excludeKey));

  const ensureMetadataLoaded = () => {
    if (metadataComplete || !metadataUrl) {
      return Promise.resolve();
    }
    if (!metadataLoadPromise) {
      metadataLoadPromise = fetch(metadataUrl + location.search, {
        headers: { Accept: "application/json" },
      })
        .then((response) => {
          if (!response.ok) {
            throw new Error("HTTP " + response.status);
          }
          return response.json();
        })
        .then((data) => {
          pageData = Array.isArray(data) ? data : [];
          applyPageData(pageData);
          rowStates.forEach(applyMetadataToState);
          metadataComplete = true;
          bootstrapEl.dataset.metadataComplete = "true";
        })
        .catch((error) => {
          metadataLoadPromise = null;
          console.warn("히스토리 필터 메타데이터를 불러오지 못했습니다.", error);
        });
    }
    return metadataLoadPromise;
  };

  const populateSelect = (select, values, currentValue) => {
    if (!select) {
      return;
    }

    select.innerHTML = '<option value="">전체</option>';

    const normalizedValues = [...values];
    if (currentValue && !normalizedValues.includes(currentValue)) {
      normalizedValues.unshift(currentValue);
    }

    normalizedValues.forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      select.appendChild(option);
    });

    select.value = currentValue || "";
  };

  const syncSelectOptions = (overrides = {}) => {
    const methodValues = uniqueSorted(
      visibleStates("method").flatMap((state) => state.methods),
      compareMethods,
    );
    const httpStatusValues = uniqueSorted(
      visibleStates("httpStatus").flatMap((state) => state.httpStatuses),
      (left, right) => Number(left) - Number(right),
    );
    const currentMethod =
      overrides.method !== undefined ? overrides.method : methodFilter ? methodFilter.value : "";
    const currentHttpStatus =
      overrides.httpStatus !== undefined
        ? overrides.httpStatus
        : httpStatusFilter
          ? httpStatusFilter.value
          : "";

    populateSelect(methodFilter, methodValues, currentMethod);
    populateSelect(httpStatusFilter, httpStatusValues, currentHttpStatus);
  };

  const autocompleteConfigs = [
    {
      key: "api",
      input: apiInput,
      panel: apiPanel,
      badge: "API",
      emptyText: "일치하는 API가 없습니다.",
      collect: (states) => states.flatMap((state) => state.apis),
      compare: compareText,
    },
    {
      key: "caseName",
      input: caseNameInput,
      panel: caseNamePanel,
      badge: "CASE",
      emptyText: "일치하는 테스트 케이스가 없습니다.",
      collect: (states) => states.flatMap((state) => state.caseNames),
      compare: compareText,
    },
    {
      key: "endpoint",
      input: endpointInput,
      panel: endpointPanel,
      badge: "ENDPOINT",
      emptyText: "일치하는 엔드포인트가 없습니다.",
      collect: (states) => states.flatMap((state) => state.endpoints),
      compare: compareText,
    },
  ].filter((config) => config.input && config.panel);

  const closePanels = () => {
    autocompleteConfigs.forEach(({ panel }) => {
      panel.hidden = true;
      panel.innerHTML = "";
    });
    activeAutocomplete = null;
    activeIndex = -1;
    currentOptions = [];
  };

  const setActiveOption = (nextIndex) => {
    if (!activeAutocomplete) {
      return;
    }
    const buttons = Array.from(activeAutocomplete.panel.querySelectorAll(".autocomplete-option"));
    buttons.forEach((button) => button.classList.remove("is-active"));
    if (nextIndex < 0 || nextIndex >= buttons.length) {
      activeIndex = -1;
      return;
    }
    activeIndex = nextIndex;
    buttons[activeIndex].classList.add("is-active");
    buttons[activeIndex].scrollIntoView({ block: "nearest" });
  };

  const buildOptions = (config) => {
    const keyword = normalize(config.input.value);
    return uniqueSorted(config.collect(visibleStates(config.key)), config.compare)
      .filter((value) => !keyword || normalize(value).includes(keyword))
      .slice(0, maxAutocompleteOptions);
  };

  const renderAutocomplete = (config) => {
    if (!config) {
      closePanels();
      return;
    }

    currentOptions = buildOptions(config);
    activeAutocomplete = config;
    activeIndex = -1;

    autocompleteConfigs.forEach(({ panel }) => {
      if (panel !== config.panel) {
        panel.hidden = true;
        panel.innerHTML = "";
      }
    });

    config.panel.innerHTML = "";

    if (currentOptions.length === 0) {
      config.panel.innerHTML = '<div class="autocomplete-empty">' + config.emptyText + "</div>";
      config.panel.hidden = false;
      return;
    }

    currentOptions.forEach((option, index) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "autocomplete-option";
      button.setAttribute("data-index", String(index));
      button.innerHTML =
        '<span class="autocomplete-option-badge">' +
        config.badge +
        "</span>" +
        '<span class="autocomplete-option-text">' +
        escapeHtml(option) +
        "</span>";
      button.addEventListener("mousedown", (event) => event.preventDefault());
      button.addEventListener("click", () => {
        config.input.value = option;
        closePanels();
        refresh();
      });
      config.panel.appendChild(button);
    });

    config.panel.hidden = false;
  };

  const updateUrl = () => {
    const next = new URLSearchParams();

    if (apiInput.value.trim()) {
      next.set("api", apiInput.value.trim());
    }
    if (sourceFilter && sourceFilter.value) {
      next.set("source", sourceFilter.value);
    }
    if (statusFilter && statusFilter.value) {
      next.set("status", statusFilter.value);
    }
    if (methodFilter && methodFilter.value) {
      next.set("method", methodFilter.value);
    }
    if (httpStatusFilter && httpStatusFilter.value) {
      next.set("httpStatus", httpStatusFilter.value);
    }
    if (caseNameInput && caseNameInput.value.trim()) {
      next.set("caseName", caseNameInput.value.trim());
    }
    if (endpointInput && endpointInput.value.trim()) {
      next.set("endpoint", endpointInput.value.trim());
    }
    if (fromInput && fromInput.value) {
      next.set("from", fromInput.value);
    }
    if (toInput && toInput.value) {
      next.set("to", toInput.value);
    }

    const search = next.toString() ? "?" + next.toString() : "";
    history.replaceState(null, "", location.pathname + search);
  };

  const filterRows = () => {
    let visible = 0;

    rowStates.forEach((state) => {
      const match = matchesFilters(state);
      state.row.style.display = match ? "" : "none";
      if (match) {
        visible += 1;
      }
    });

    if (countEl) {
      countEl.textContent = visible + "개 배치 실행";
    }
    if (emptyState) {
      emptyState.style.display = visible === 0 ? "" : "none";
    }
    if (tableWrapper) {
      tableWrapper.classList.toggle("is-hidden", visible === 0);
    }

    updateUrl();
  };

  const refresh = () => {
    const activeConfig = activeAutocomplete;
    syncSelectOptions();
    filterRows();
    if (activeConfig && document.activeElement === activeConfig.input) {
      renderAutocomplete(activeConfig);
    }
  };

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    refresh();
  });

  const urlParams = new URLSearchParams(location.search);
  const initialMethod = urlParams.get("method") || "";
  const initialHttpStatus = urlParams.get("httpStatus") || "";

  if (apiInput && urlParams.get("api")) {
    apiInput.value = urlParams.get("api");
  }
  if (sourceFilter && urlParams.get("source")) {
    sourceFilter.value = urlParams.get("source");
  }
  if (statusFilter && urlParams.get("status")) {
    statusFilter.value = urlParams.get("status");
  }
  if (caseNameInput && urlParams.get("caseName")) {
    caseNameInput.value = urlParams.get("caseName");
  }
  if (endpointInput && urlParams.get("endpoint")) {
    endpointInput.value = urlParams.get("endpoint");
  }
  if (fromInput && urlParams.get("from")) {
    fromInput.value = urlParams.get("from");
  }
  if (toInput && urlParams.get("to")) {
    toInput.value = urlParams.get("to");
  }

  syncSelectOptions({ method: initialMethod, httpStatus: initialHttpStatus });

  if (methodFilter) {
    methodFilter.value = initialMethod;
  }
  if (httpStatusFilter) {
    httpStatusFilter.value = initialHttpStatus;
  }

  refresh();

  if (sourceFilter) {
    sourceFilter.addEventListener("change", refresh);
  }
  if (statusFilter) {
    statusFilter.addEventListener("change", refresh);
  }
  if (methodFilter) {
    methodFilter.addEventListener("focus", () => {
      ensureMetadataLoaded().then(syncSelectOptions);
    });
    methodFilter.addEventListener("mousedown", () => {
      ensureMetadataLoaded().then(syncSelectOptions);
    });
    methodFilter.addEventListener("change", refresh);
  }
  if (httpStatusFilter) {
    httpStatusFilter.addEventListener("focus", () => {
      ensureMetadataLoaded().then(syncSelectOptions);
    });
    httpStatusFilter.addEventListener("mousedown", () => {
      ensureMetadataLoaded().then(syncSelectOptions);
    });
    httpStatusFilter.addEventListener("change", refresh);
  }
  if (fromInput) {
    fromInput.addEventListener("change", refresh);
  }
  if (toInput) {
    toInput.addEventListener("change", refresh);
  }

  autocompleteConfigs.forEach((config) => {
    config.input.addEventListener("focus", () => {
      ensureMetadataLoaded().then(() => renderAutocomplete(config));
    });

    config.input.addEventListener("input", () => {
      ensureMetadataLoaded().then(() => {
        renderAutocomplete(config);
        refresh();
      });
    });

    config.input.addEventListener("keydown", (event) => {
      const panelHidden = config.panel.hidden || activeAutocomplete !== config;
      if (panelHidden && (event.key === "ArrowDown" || event.key === "ArrowUp")) {
        renderAutocomplete(config);
        event.preventDefault();
        return;
      }

      if (panelHidden) {
        if (event.key === "Escape") {
          closePanels();
        }
        return;
      }

      if (event.key === "ArrowDown") {
        event.preventDefault();
        setActiveOption(activeIndex + 1 >= currentOptions.length ? 0 : activeIndex + 1);
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        setActiveOption(activeIndex - 1 < 0 ? currentOptions.length - 1 : activeIndex - 1);
      } else if (event.key === "Enter" && activeIndex >= 0 && currentOptions[activeIndex]) {
        event.preventDefault();
        config.input.value = currentOptions[activeIndex];
        closePanels();
        refresh();
      } else if (event.key === "Escape") {
        closePanels();
      }
    });
  });

  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      if (sourceFilter) {
        sourceFilter.value = "";
      }
      if (statusFilter) {
        statusFilter.value = "";
      }
      if (methodFilter) {
        methodFilter.value = "";
      }
      if (httpStatusFilter) {
        httpStatusFilter.value = "";
      }
      if (caseNameInput) {
        caseNameInput.value = "";
      }
      if (endpointInput) {
        endpointInput.value = "";
      }
      if (fromInput) {
        fromInput.value = "";
      }
      if (toInput) {
        toInput.value = "";
      }
      apiInput.value = "";
      closePanels();
      refresh();
    });
  }

  document.addEventListener("click", (event) => {
    const clickedAutocomplete = autocompleteConfigs.some(
      (config) =>
        config.input === event.target ||
        config.panel === event.target ||
        config.panel.contains(event.target),
    );
    if (!clickedAutocomplete) {
      closePanels();
    }
  });
})();
