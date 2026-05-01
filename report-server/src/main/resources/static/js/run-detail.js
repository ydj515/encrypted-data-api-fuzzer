(function () {
  const bootstrapEl = document.getElementById("runDetailPageData");
  const bootstrapApis = bootstrapEl ? JSON.parse(bootstrapEl.textContent || "[]") : [];

  const statusFilter = document.getElementById("statusFilter");
  const kindFilter = document.getElementById("kindFilter");
  const apiInput = document.getElementById("apiFilterInput");
  const apiPanel = document.getElementById("apiAutocompletePanel");
  const caseNameInput = document.getElementById("caseNameFilterInput");
  const caseNamePanel = document.getElementById("caseNameAutocompletePanel");
  const endpointInput = document.getElementById("endpointFilterInput");
  const endpointPanel = document.getElementById("endpointAutocompletePanel");
  const methodFilter = document.getElementById("methodFilter");
  const httpStatusFilter = document.getElementById("httpStatusFilter");
  const resetBtn = document.getElementById("resetFilterBtn");
  const caseCountEl = document.getElementById("caseCount");
  const emptyState = document.getElementById("emptyState");
  const casesTable = document.getElementById("casesTable");
  const tableWrapper = casesTable ? casesTable.closest(".table-wrapper") : null;
  const rows = casesTable ? Array.from(casesTable.querySelectorAll("tbody tr")) : [];
  const failurePopover = document.getElementById("failurePopover");

  if (!casesTable || !apiPanel || !apiInput) {
    return;
  }

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

  const createSearchableList = (...values) => {
    const items = toTextList(values);
    return {
      values: items,
      normalized: items.map((value) => normalize(value)),
    };
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

  const rowStates = rows.map((row) => {
    const api = createSearchableList(row.dataset.api || "");
    const caseNames = createSearchableList(row.dataset.name || "", row.dataset.scenarioName || "");
    const endpoints = createSearchableList(row.dataset.endpoint || "");

    return {
      row,
      status: row.dataset.status || "",
      kind: row.dataset.kind || "",
      api: api.values[0] || "",
      apiNormalized: api.normalized[0] || "",
      caseNames: caseNames.values,
      caseNamesNormalized: caseNames.normalized,
      endpoint: endpoints.values[0] || "",
      endpointNormalized: endpoints.normalized[0] || "",
      method: (row.dataset.method || "").toUpperCase(),
      httpStatus: row.dataset.httpStatus || "",
    };
  });

  const textMatches = (query, normalizedValues) => {
    if (!query) {
      return true;
    }
    return normalizedValues.some((value) => value.includes(query));
  };

  const currentFilters = () => ({
    status: statusFilter ? statusFilter.value : "",
    kind: kindFilter ? kindFilter.value : "",
    api: normalize(apiInput ? apiInput.value : ""),
    caseName: normalize(caseNameInput ? caseNameInput.value : ""),
    endpoint: normalize(endpointInput ? endpointInput.value : ""),
    method: methodFilter ? methodFilter.value : "",
    httpStatus: httpStatusFilter ? httpStatusFilter.value : "",
  });

  const matchesFilters = (state, excludeKey) => {
    const filters = currentFilters();

    if (excludeKey !== "status" && filters.status && state.status !== filters.status) {
      return false;
    }
    if (excludeKey !== "kind" && filters.kind && state.kind !== filters.kind) {
      return false;
    }
    if (excludeKey !== "api" && !textMatches(filters.api, [state.apiNormalized])) {
      return false;
    }
    if (excludeKey !== "caseName" && !textMatches(filters.caseName, state.caseNamesNormalized)) {
      return false;
    }
    if (excludeKey !== "endpoint" && !textMatches(filters.endpoint, [state.endpointNormalized])) {
      return false;
    }
    if (excludeKey !== "method" && filters.method && state.method !== filters.method) {
      return false;
    }
    if (excludeKey !== "httpStatus" && filters.httpStatus && state.httpStatus !== filters.httpStatus) {
      return false;
    }
    return true;
  };

  const visibleStates = (excludeKey) => rowStates.filter((state) => matchesFilters(state, excludeKey));

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
      visibleStates("method").map((state) => state.method),
      compareMethods,
    );
    const httpStatusValues = uniqueSorted(
      visibleStates("httpStatus").map((state) => state.httpStatus),
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
      collect: (states) => uniqueSorted([...bootstrapApis, ...states.map((state) => state.api)], compareText),
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
      collect: (states) => states.map((state) => state.endpoint),
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
    const candidates = config.collect(visibleStates(config.key));
    return uniqueSorted(candidates, config.compare)
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
        syncSelectOptions();
        filterRows();
      });
      config.panel.appendChild(button);
    });

    config.panel.hidden = false;
  };

  const closeFailurePopover = () => {
    if (failurePopover) {
      failurePopover.hidden = true;
      failurePopover._trigger = null;
    }
  };

  const updateUrl = () => {
    const next = new URLSearchParams();
    const filters = currentFilters();

    if (filters.status) {
      next.set("status", filters.status);
    }
    if (filters.kind) {
      next.set("kind", filters.kind);
    }
    if (apiInput && apiInput.value.trim()) {
      next.set("api", apiInput.value.trim());
    }
    if (caseNameInput && caseNameInput.value.trim()) {
      next.set("caseName", caseNameInput.value.trim());
    }
    if (endpointInput && endpointInput.value.trim()) {
      next.set("endpoint", endpointInput.value.trim());
    }
    if (filters.method) {
      next.set("method", filters.method);
    }
    if (filters.httpStatus) {
      next.set("httpStatus", filters.httpStatus);
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

    if (caseCountEl) {
      caseCountEl.textContent = visible + "개 케이스";
    }
    if (emptyState) {
      emptyState.style.display = visible === 0 ? "" : "none";
    }
    if (tableWrapper) {
      tableWrapper.style.display = visible === 0 ? "none" : "";
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

  const openFailurePopover = (trigger) => {
    if (!failurePopover) {
      return;
    }

    failurePopover.textContent = trigger.dataset.msg || "";
    failurePopover.hidden = false;
    failurePopover._trigger = trigger;

    const rect = trigger.getBoundingClientRect();
    failurePopover.style.top = "0";
    failurePopover.style.left = "0";

    const popoverWidth = failurePopover.offsetWidth;
    const popoverHeight = failurePopover.offsetHeight;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let top = rect.bottom + 8;
    let left = rect.left;

    if (left + popoverWidth > viewportWidth - 12) {
      left = Math.max(8, viewportWidth - popoverWidth - 12);
    }
    if (top + popoverHeight > viewportHeight - 12) {
      top = Math.max(8, rect.top - popoverHeight - 8);
    }

    failurePopover.style.top = top + "px";
    failurePopover.style.left = left + "px";
  };

  const urlParams = new URLSearchParams(location.search);
  const initialMethod = urlParams.get("method") || "";
  const initialHttpStatus = urlParams.get("httpStatus") || "";

  syncSelectOptions({ method: initialMethod, httpStatus: initialHttpStatus });

  if (statusFilter && urlParams.get("status")) {
    statusFilter.value = urlParams.get("status");
  }
  if (kindFilter && urlParams.get("kind")) {
    kindFilter.value = urlParams.get("kind");
  }
  if (apiInput && urlParams.get("api")) {
    apiInput.value = urlParams.get("api");
  }
  if (caseNameInput && urlParams.get("caseName")) {
    caseNameInput.value = urlParams.get("caseName");
  }
  if (endpointInput && urlParams.get("endpoint")) {
    endpointInput.value = urlParams.get("endpoint");
  }
  if (methodFilter) {
    methodFilter.value = initialMethod;
  }
  if (httpStatusFilter) {
    httpStatusFilter.value = initialHttpStatus;
  }

  syncSelectOptions();
  filterRows();

  if (statusFilter) {
    statusFilter.addEventListener("change", refresh);
  }
  if (kindFilter) {
    kindFilter.addEventListener("change", refresh);
  }
  if (methodFilter) {
    methodFilter.addEventListener("change", refresh);
  }
  if (httpStatusFilter) {
    httpStatusFilter.addEventListener("change", refresh);
  }

  autocompleteConfigs.forEach((config) => {
    config.input.addEventListener("focus", () => {
      renderAutocomplete(config);
    });

    config.input.addEventListener("input", () => {
      renderAutocomplete(config);
      refresh();
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
      if (statusFilter) {
        statusFilter.value = "";
      }
      if (kindFilter) {
        kindFilter.value = "";
      }
      if (methodFilter) {
        methodFilter.value = "";
      }
      if (httpStatusFilter) {
        httpStatusFilter.value = "";
      }
      apiInput.value = "";
      if (caseNameInput) {
        caseNameInput.value = "";
      }
      if (endpointInput) {
        endpointInput.value = "";
      }
      closePanels();
      closeFailurePopover();
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

    const failureTrigger = event.target.closest(".failure-msg");
    if (failureTrigger && failurePopover) {
      event.stopPropagation();
      if (!failurePopover.hidden && failurePopover._trigger === failureTrigger) {
        closeFailurePopover();
      } else {
        openFailurePopover(failureTrigger);
      }
    } else if (failurePopover && !failurePopover.hidden && !failurePopover.contains(event.target)) {
      closeFailurePopover();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeFailurePopover();
    }
  });
})();
