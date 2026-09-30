import { ThreeUIIntro } from "./neuform-isolated/NeuformIsolatedEffects";

export { PredictiveArcCanvas } from "./predictive-arc/PredictiveArcCollection";

type TextAnimationVariant = "threeui-intro";
type EffectMode = "light" | "dark";

export function TextAnimationCollection({
  variant = "threeui-intro",
  mode = "dark",
  hue = 0,
  saturation = 1,
  brightness = 1,
}: {
  variant?: TextAnimationVariant;
  mode?: EffectMode;
  hue?: number;
  saturation?: number;
  brightness?: number;
}) {
  if (variant !== "threeui-intro") return null;

  return (
    <ThreeUIIntro
      mode={mode}
      hue={hue}
      saturation={saturation}
      brightness={brightness}
      className="h-full w-full"
    />
  );
}
