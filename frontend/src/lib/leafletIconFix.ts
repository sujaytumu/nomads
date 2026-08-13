import L from "leaflet";
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

// Leaflet's default marker icon paths are computed relative to the CSS file
// location, which breaks under Next.js/Webpack bundling (icons silently
// 404 and markers show as broken image outlines). This re-points them at
// the bundler-resolved asset URLs. Must be imported once before any L.marker()
// or L.map() call - imported from each map component for that reason.
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x.src,
  iconUrl: markerIcon.src,
  shadowUrl: markerShadow.src,
});
