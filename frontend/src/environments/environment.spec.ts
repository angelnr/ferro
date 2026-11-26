import { devApiBaseUrl } from './environment';

describe('devApiBaseUrl', () => {
  it('points the API at the host that serves the app', () => {
    expect(devApiBaseUrl('100.90.74.104')).toBe('http://100.90.74.104:8082/api/v1');
  });

  it('keeps localhost when the app is served locally', () => {
    expect(devApiBaseUrl('localhost')).toBe('http://localhost:8082/api/v1');
  });

  it('falls back to localhost when the hostname is empty', () => {
    expect(devApiBaseUrl('   ')).toBe('http://localhost:8082/api/v1');
  });
});
