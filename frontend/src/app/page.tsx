import HomeDashboard from "@/components/HomeDashboard";
import HomePackages from "@/components/HomePackages";
import PickedForYou from "@/components/PickedForYou";

// No server-side data fetch here: the page shell is instant and each block loads in the
// browser, so a sleeping backend can no longer hold the whole home screen hostage.
export default function HomePage() {
  return (
    <div>
      <HomeDashboard />
      <PickedForYou />
      <HomePackages />
    </div>
  );
}
