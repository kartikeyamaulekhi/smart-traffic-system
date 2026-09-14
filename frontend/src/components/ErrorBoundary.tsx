import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('UI crash:', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="crash-screen">
          <div className="crash-card glass">
            <div className="crash-icon">⚠️</div>
            <h2>Something went wrong</h2>
            <p className="muted">An unexpected error occurred while rendering this view.</p>
            <pre className="crash-detail">{this.state.error.message}</pre>
            <button
              className="btn-primary"
              onClick={() => {
                this.setState({ error: null });
                window.location.hash = '';
              }}
            >
              Reload view
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}