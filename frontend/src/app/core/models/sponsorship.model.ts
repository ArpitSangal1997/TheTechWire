export interface Sponsorship {
  id: number;
  brandName: string;
  headline: string;
  targetUrl: string;
  creativeImageUrl?: string;
  placement: 'FEED_BANNER' | 'SIDEBAR' | 'NEWS_TICKER' | 'POST_INLINE';
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ENDED';
  startDate: string;
  endDate: string;
  impressions: number;
  clicks: number;
}
