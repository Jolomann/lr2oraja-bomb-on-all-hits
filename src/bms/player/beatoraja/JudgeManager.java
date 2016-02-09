package bms.player.beatoraja;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

/**
 * ノーツ判定管理用クラス
 * 
 * @author exch
 */
public class JudgeManager {

	private BMSPlayer main;
	private BMSModel model;

	/**
	 * 現在の判定カウント内訳
	 */
	private int[][] count = new int[6][2];

	/**
	 * 現在のコンボ数
	 */
	private int combo;
	/**
	 * 最大コンボ数
	 */
	private int maxcombo;
	/**
	 * 現在表示中の判定
	 */
	private int judgenow;
	/**
	 * 判定の最終更新時間
	 */
	private int judgenowt;
	/**
	 * 判定差時間(ms , +は早押しで-は遅押し)
	 */
	private int judgefast;
	/**
	 * ボムの表示開始時間
	 */
	private long[] bomb = new long[8];
	/**
	 * 処理中のLN
	 */
	private LongNote[] processing = new LongNote[8];
	private int sckey;
	/**
	 * ミスレイヤー表示開始時間
	 */
	private int misslayer;

	private final int[][] judgetable = new int[][] {
			{ 8, 24, 70, 130, 0, 1000 }, { 14, 42, 115, 220, 0, 1000 },
			{ 18, 54, 150, 285, 0, 1000 }, { 20, 60, 165, 315, 0, 1000 } };

	public JudgeManager(BMSPlayer main, BMSModel model) {
		this.main = main;
		this.model = model;
		Arrays.fill(bomb, -1000);
		if (model.getJudgerank() > 3) {
			judge = judgetable[3];
		} else {
			judge = judgetable[model.getJudgerank()];
		}
	}

	private int[] judge;

	private int pos = 0;
	private int judgetype = 0;

	public void update(TimeLine[] timelines, int time) {
		BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
		long[] keytime = input.getTime();
		boolean[] keystate = input.getKeystate();

		for (int key = 0; key < 9; key++) {
			if (keytime[key] != 0) {
				long ptime = keytime[key];
				int lane = key == 8 ? 7 : key;
				if (keystate[key]) {
					// キーが押されたときの処理
					if (processing[lane] != null) {
						if (lane == 7 && key != sckey) {
							for (int j = 0; j < judge.length; j++) {
								if (j > 3) {
									j = 4;
								}
								if (j == 4
										|| (ptime > processing[lane].getEnd()
												.getTime() - judge[j] && ptime < processing[lane]
												.getEnd().getTime() + judge[j])) {
									if (j < 2) {
										bomb[lane] = ptime;
									}
									this.update(
											j < 4 ? j : 4,
											time,
											(int) (processing[lane].getEnd().getTime() - ptime));
									main.update(j);
									processing[lane].setState(j + 1);
									// System.out.println("打鍵:" + time +
									// " ノーツ位置 > " + tl.getTime() + " -
									// " +
									// (lane + 1) + " : " +
									// judgename[j]);
									System.out.println("BSS終端判定 - Time : " + ptime
											+ " Judge : " + (judgenow - 1) + " LN : "
											+ processing[lane].hashCode());
									processing[lane] = null;
									sckey = 0;
									break;
								}
							}

						} else {
							// ここに来るのはマルチキーアサイン以外ありえないはず
						}
					} else {
						TimeLine tl = null;
						int j = 0;
						// 対象ノーツの抽出
						for (int i = pos; i < timelines.length
								&& timelines[i].getTime() < ptime + judge[5]; i++) {
							if (timelines[i].getTime() >= ptime - judge[5]) {
								Note judgenote = timelines[i].getNote(lane);
								if (judgenote != null
										&& (judgenote.getState() == 0 || timelines[i]
												.getTime() < ptime - judge[3])) {
									if (tl == null) {
										tl = timelines[i];
										for (j = 0; j < judge.length
												&& !(ptime >= timelines[i]
														.getTime() - judge[j] && ptime <= timelines[i]
														.getTime() + judge[j]); j++) {
										}
									} else {
										switch (judgetype) {
										case 0:
											if (tl.getTime() < ptime - judge[3]) {
												tl = timelines[i];
												for (j = 0; j < judge.length
														&& !(ptime >= timelines[i]
																.getTime()
																- judge[j] && ptime <= timelines[i]
																.getTime()
																+ judge[j]); j++) {
												}
											}
											break;
										case 1:
											if (Math.abs(tl.getTime() - ptime) > Math
													.abs(timelines[i].getTime()
															- ptime)) {
												tl = timelines[i];
												for (j = 0; j < judge.length
														&& !(ptime >= timelines[i]
																.getTime()
																- judge[j] && ptime <= timelines[i]
																.getTime()
																+ judge[j]); j++) {
												}
											}
											break;
										}
									}
								}
							} else {
								pos = i;
							}
						}
						if (tl != null) {
							Note note = tl.getNote(lane);
							if (note instanceof LongNote) {
								// ロングノート処理
								LongNote ln = (LongNote) note;
								if (ln.getStart() == tl) {
									main.play(note.getWav());
									if (j < 2) {
										bomb[lane] = ptime;
									}
									this.update(j, time,
											(int) (tl.getTime() - ptime));
									main.update(j);
									if (j < 4) {
										processing[lane] = ln;
										if (lane == 7) {
											// BSS処理開始
											System.out
													.println("BSS開始判定 - Time : "
															+ ptime
															+ " Judge : "
															+ j
															+ " KEY : "
															+ key
															+ " LN : "
															+ note.hashCode());
											sckey = key;

										}
									}
								}
							} else {
								main.play(note.getWav());
								// 通常ノート処理
								if (j < 2) {
									bomb[lane] = ptime;
								}
								this.update(j, time,
										(int) (tl.getTime() - ptime));
								main.update(j);
								if (j < 4) {
									note.setState(j + 1);
								}
								// System.out.println("打鍵:" + time +
								// " ノーツ位置 > " + tl.getTime() + " - " +
								// (lane + 1) + " : " + judgename[j]);
							}
						} else {
							Note n = null;
							boolean sound = false;
							for (TimeLine tl2 : timelines) {
								if (tl2.getNote(lane) != null) {
									n = tl2.getNote(lane);
									if (tl2.getTime() >= ptime) {
										main.play(n.getWav());
										sound = true;
										break;
									}
								}
							}
							if (!sound && n != null) {
								main.play(n.getWav());
							}
						}
					}
				} else {
					if (processing[lane] != null) {
						// キーが離されたときの処理
						for (int j = 0; j < judge.length; j++) {
							if (j > 3) {
								j = 4;
							}
							if (j == 4
									|| (ptime > processing[lane].getEnd()
											.getTime() - judge[j] && ptime < processing[lane]
											.getEnd().getTime() + judge[j])) {
								if (lane == 7) {
									if (j != 4) {
										break;
									}
									System.out.println("BSS途中離し判定 - Time : "
											+ ptime + " Judge : " + j
											+ " LN : " + processing[lane]);
									sckey = 0;
								}
								if (j < 2) {
									bomb[lane] = ptime;
								}
								this.update(j, time, (int) (processing[lane]
										.getEnd().getTime() - ptime));
								main.update(j);
								processing[lane].setState(j + 1);
								// System.out.println("打鍵:" + time +
								// " ノーツ位置 > " + tl.getTime() + " - " +
								// (lane + 1) + " : " + judgename[j]);
								j = judge.length;
								processing[lane] = null;
								break;
							}
						}
					}
				}
				keytime[key] = 0;
			}
		}
		for (int lane = 0; lane < 8; lane++) {
			// 見逃しPOOR判定
			for (int i = 0; i < timelines.length
					&& timelines[i].getTime() < time - judge[3]; i++) {
				if (timelines[i].getTime() >= time - judge[3] - 500) {
					Note note = timelines[i].getNote(lane);
					if (note != null && note.getState() == 0) {
						int judge = timelines[i].getTime() - time;
						if (note instanceof LongNote) {
							LongNote ln = (LongNote) note;
							if (ln.getStart() == timelines[i]) {
								if (processing[lane] != ln) {
									// System.out.println("ln start poor");
									this.update(4, time, judge);
									this.update(4, time, judge);
									main.update(4);
									main.update(4);
									note.setState(5);
								}
							} else {
								// System.out.println("ln end poor");
								this.update(4, time, judge);
								main.update(4);
								note.setState(5);
								processing[lane] = null;
								sckey = 0;
							}
						} else {
							this.update(4, time, judge);
							main.update(4);
							note.setState(5);
						}
					}
				}
			}
		}
	}

	private void update(int j, int time, int fast) {
		judgenow = j + 1;
		judgenowt = time;
		count[j][fast >= 0 ? 0 : 1]++;
		judgefast = fast;
		if (j < 3) {
			combo++;
			maxcombo = maxcombo > combo ? maxcombo : combo;
		} else if (j >= 3 && j < 5) {
			combo = 0;
			misslayer = time;
		}
	}

	public int getJudgeNow() {
		return judgenow;
	}

	public int getJudgeTime() {
		return judgenowt;
	}

	public int getRecentJudgeTiming() {
		return judgefast;
	}

	public int getCombo() {
		return combo;
	}

	public long[] getBomb() {
		return bomb;
	}

	public LongNote[] getProcessingLongNotes() {
		return processing;
	}

	public int getMisslayer() {
		return misslayer;
	}

	public int getMaxcombo() {
		return maxcombo;
	}

	public int getJudgeCount() {
		return count[0][0] + count[0][1] + count[1][0] + count[1][1]
				+ count[2][0] + count[2][1] + count[3][0] + count[3][1]
				+ count[4][0] + count[4][1] + count[5][0] + count[5][1];

	}

	public int getJudgeCount(int judge) {
		return count[judge][0] + count[judge][1];
	}

	public int getJudgeCount(int judge, boolean fast) {
		return fast ? count[judge][0] : count[judge][1];
	}

}