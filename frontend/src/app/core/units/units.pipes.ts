import { Pipe, PipeTransform, inject } from '@angular/core';
import { SessionStore } from '../auth/session.store';

const KG_TO_LB = 2.2046226;
const CM_TO_IN = 0.3937008;

@Pipe({ name: 'weight', standalone: true, pure: false })
export class WeightPipe implements PipeTransform {
  private readonly session = inject(SessionStore);

  transform(value: number | string | null | undefined, decimals = 1): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    const numeric = Number(value);
    const imperial = this.session.user()?.unitSystem === 'LB';
    const locale = this.session.user()?.locale ?? 'es';
    const converted = imperial ? numeric * KG_TO_LB : numeric;
    const formatted = new Intl.NumberFormat(locale, {
      minimumFractionDigits: imperial ? decimals : 0,
      maximumFractionDigits: imperial ? decimals : 2,
    }).format(converted);
    return `${formatted} ${imperial ? 'lb' : 'kg'}`;
  }
}

@Pipe({ name: 'length', standalone: true, pure: false })
export class LengthPipe implements PipeTransform {
  private readonly session = inject(SessionStore);

  transform(value: number | string | null | undefined, decimals = 1): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    const numeric = Number(value);
    const imperial = this.session.user()?.unitSystem === 'LB';
    const locale = this.session.user()?.locale ?? 'es';
    const converted = imperial ? numeric * CM_TO_IN : numeric;
    const formatted = new Intl.NumberFormat(locale, {
      minimumFractionDigits: 0,
      maximumFractionDigits: decimals,
    }).format(converted);
    return `${formatted} ${imperial ? 'in' : 'cm'}`;
  }
}
