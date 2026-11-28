import { Injectable } from '@angular/core';

export interface CatalogFilters {
  q: string;
  muscle: string;
  equipment: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogFilterStateService {
  private readonly key = 'ferro.catalog.filters';
  private readonly ttlMs = 5 * 60 * 1000;

  load(): CatalogFilters | null {
    try {
      const raw = sessionStorage.getItem(this.key);
      if (!raw) {
        return null;
      }
      const state = JSON.parse(raw) as CatalogFilters & { savedAt: number };
      if (!state.savedAt || Date.now() - state.savedAt > this.ttlMs) {
        sessionStorage.removeItem(this.key);
        return null;
      }
      return { q: state.q ?? '', muscle: state.muscle ?? '', equipment: state.equipment ?? '' };
    } catch {
      return null;
    }
  }

  save(filters: CatalogFilters): void {
    try {
      sessionStorage.setItem(this.key, JSON.stringify({ ...filters, savedAt: Date.now() }));
    } catch {
      // sessionStorage puede no estar disponible en algunos navegadores/contextos.
    }
  }
}
