import { TestBed } from '@angular/core/testing';
import { CatalogFilterStateService } from './catalog-filter-state.service';

describe('CatalogFilterStateService', () => {
  let service: CatalogFilterStateService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CatalogFilterStateService);
  });

  afterEach(() => sessionStorage.clear());

  it('saves and restores filters for five minutes', () => {
    service.save({ q: 'press', muscle: 'CHEST', equipment: 'BARBELL' });
    expect(service.load()).toEqual({ q: 'press', muscle: 'CHEST', equipment: 'BARBELL' });

    vi.useFakeTimers();
    vi.setSystemTime(Date.now() + 5 * 60 * 1000 + 1);
    expect(service.load()).toBeNull();
    vi.useRealTimers();
  });

  it('ignores malformed state', () => {
    sessionStorage.setItem('ferro.catalog.filters', '{bad');
    expect(service.load()).toBeNull();
  });
});
