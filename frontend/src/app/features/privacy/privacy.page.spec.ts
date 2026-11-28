import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { PrivacyPage } from './privacy.page';

describe('PrivacyPage', () => {
  it('renders the privacy screen', () => {
    TestBed.configureTestingModule({
      imports: [PrivacyPage],
      providers: [provideRouter([]), ...provideTranslateService({ lang: 'es' })],
    });
    const fixture = TestBed.createComponent(PrivacyPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('privacy.title');
  });
});
