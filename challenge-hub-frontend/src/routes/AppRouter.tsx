import { Navigate, Route, Routes } from "react-router-dom";
import { AuthGuard } from "./guards/AuthGuard";
import { RoleGuard } from "./guards/RoleGuard";
import { ROUTE_PATHS } from "./paths";
import { routeConfig, type RouteConfigItem } from "./routeConfig";

const buildRouteElement = (route: RouteConfigItem) => {
  let element = route.element;

  if (route.allowedRoles && route.allowedRoles.length > 0) {
    element = (
      <RoleGuard allowedRoles={route.allowedRoles}>{element}</RoleGuard>
    );
  }

  if (route.requiresAuth) {
    element = <AuthGuard>{element}</AuthGuard>;
  }

  return element;
};

export const AppRouter = () => {
  return (
    <Routes>
      <Route
        path={ROUTE_PATHS.ROOT}
        element={<Navigate to={ROUTE_PATHS.CHALLENGES} replace />}
      />

      {routeConfig.map((route) => (
        <Route
          key={route.path}
          path={route.path}
          element={buildRouteElement(route)}
        />
      ))}

      <Route path="*" element={<Navigate to={ROUTE_PATHS.ROOT} replace />} />
    </Routes>
  );
};
