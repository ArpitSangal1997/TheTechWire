export interface NewsHeadline {
  id: number;
  title: string;
  sourceUrl: string;
  sourceName: string;
  imageUrl?: string;
  description?: string;
  category: string;
  publishedAt: string;
}
