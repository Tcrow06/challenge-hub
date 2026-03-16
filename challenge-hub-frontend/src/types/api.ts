export interface FieldError {
  field: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  errorCode?: string;
  errors?: FieldError[];
  metadata: Record<string, unknown> | null;
  timestamp: string;
}
