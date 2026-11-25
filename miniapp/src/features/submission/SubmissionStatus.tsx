type Props = { error: string | null; success: string | null };
export function SubmissionStatus({ error, success }: Props) {
  return <>{error && <p className="error-message" role="alert">{error}</p>}{success && <p className="success-message" role="status">{success}</p>}</>;
}
