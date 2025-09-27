import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './hooks/useAuth';

// Pages
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import ProductsPage from './features/products/ProductsPage';
import CartPage from './features/cart/CartPage';
import OrdersPage from './features/orders/OrdersPage';
import AdminDashboard from './features/admin/AdminDashboard';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function AppRoutes() {
  const { isAuthenticated, isManager } = useAuth();

  return (
    <Routes>
      {/* Rotas públicas */}
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" /> : <LoginPage />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to="/" /> : <RegisterPage />} />
      <Route path="/produtos" element={<ProductsPage />} />
      
      {/* Rotas protegidas - Cliente */}
      <Route
        path="/carrinho"
        element={
          <ProtectedRoute requiredRole="CUSTOMER">
            <CartPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/pedidos"
        element={
          <ProtectedRoute requiredRole="CUSTOMER">
            <OrdersPage />
          </ProtectedRoute>
        }
      />
      
      {/* Rotas protegidas - Admin */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute requiredRole="MANAGER">
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      
      {/* Rota principal */}
      <Route
        path="/"
        element={
          isAuthenticated ? (
            isManager ? (
              <Navigate to="/admin" />
            ) : (
              <Navigate to="/produtos" />
            )
          ) : (
            <Navigate to="/login" />
          )
        }
      />
      
      {/* Página de não autorizado */}
      <Route path="/unauthorized" element={<div>Acesso negado</div>} />
      
      {/* 404 */}
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="min-h-screen bg-gray-50">
          <AppRoutes />
        </div>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
