package faithcoderlab.newdpraise.domain.song;

public class PitchDistribution {
  private static final int PITCH_CLASSES = 12;
  private final int[] distribution;

  public PitchDistribution() {
    this.distribution = new int[PITCH_CLASSES];
  }

  public void addPitchClass(int pitchClass) {
    if (pitchClass >= 0 && pitchClass < PITCH_CLASSES) {
      distribution[pitchClass]++;
    }
  }

  public int getDominantPitchClass() {
    int maxCount = -1;
    int dominantPitchClass = 0;

    for (int i = 0; i < PITCH_CLASSES; i++) {
      if (distribution[i] > maxCount) {
        maxCount = distribution[i];
        dominantPitchClass = i;
      }
    }

    return dominantPitchClass;
  }

  public boolean isMajor() {
    int dominantPitchClass = getDominantPitchClass();
    int majorThirdIndex = (dominantPitchClass + 4) % PITCH_CLASSES;
    int minorThirdIndex = (dominantPitchClass + 3) % PITCH_CLASSES;

    return distribution[majorThirdIndex] > distribution[minorThirdIndex];
  }

  public int getCount(int pitchClass) {
    if (pitchClass >= 0 && pitchClass < PITCH_CLASSES) {
      return distribution[pitchClass];
    }
    return 0;
  }

  public boolean hasEnoughData() {
    int totalCount = 0;
    for (int count : distribution) {
      totalCount += count;
    }
    return totalCount >= 100;
  }
}
