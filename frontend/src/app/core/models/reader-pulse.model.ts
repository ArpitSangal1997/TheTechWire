export interface ReaderPulseOption {
  choice: string;
  label: string;
  count: number;
  percentage: number;
}

export interface ReaderPulse {
  question: string;
  totalVotes: number;
  options: ReaderPulseOption[];
}
