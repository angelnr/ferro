import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({ name: 'catalogLabel', standalone: true, pure: false })
export class CatalogLabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  transform(value: string, type: 'muscle' | 'equipment' | 'mechanics'): string {
    if (!value) {
      return '';
    }
    const key = `catalog.${type}Values.${value}`;
    const translated = this.translate.instant(key);
    return translated === key ? value : translated;
  }
}
