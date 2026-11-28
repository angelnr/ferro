import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { CatalogLabelPipe } from './catalog-label.pipe';

describe('CatalogLabelPipe', () => {
  it('falls back to the raw enum when the translation is not loaded', () => {
    TestBed.configureTestingModule({ providers: [...provideTranslateService({ lang: 'es' })] });
    const pipe = TestBed.runInInjectionContext(() => new CatalogLabelPipe());
    expect(pipe.transform('CHEST', 'muscle')).toBe('CHEST');
    expect(pipe.transform('', 'equipment')).toBe('');
  });
});
