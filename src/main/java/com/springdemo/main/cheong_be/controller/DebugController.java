package com.springdemo.main.cheong_be.controller;

import com.springdemo.main.cheong_be.model.*;
import com.springdemo.main.cheong_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final WordRepository wordRepository;
    private final DailyLogRepository dailyLogRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final UserProgressRepository userProgressRepository;

    // 더미 데이터 전용 내부 클래스
    record DummyItem(String word, String meaning, String userSentence, String aiEvaluation, List<String> aiExamples) {}

    /**
     * [타임머신 API - 리얼 오답 포함 버전]
     * POST /api/debug/seed/7
     * - 7일치(21개) 데이터를 생성합니다.
     * - 개중에는 유저가 틀리게 쓰고, AI가 교정해주는 데이터도 포함됩니다.
     */
    @PostMapping("/seed/{days}")
    @Transactional
    public String seedPastData(
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId,
            @PathVariable int days
    ) {
        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<DummyItem> realData = new ArrayList<>();

        // ==========================================
        // ✅ [정답 케이스] (잘 쓴 문장들)
        // ==========================================
        realData.add(new DummyItem("개벽", "새로운 세상이 열림", "스마트폰의 등장은 천지개벽할 일이었다.", "훌륭합니다. '천지개벽'이라는 관용구를 적절히 사용하여 변화의 크기를 잘 강조했습니다.", List.of("산업혁명은 인류 역사에 개벽을 가져왔다.")));
        realData.add(new DummyItem("관조", "고요한 마음으로 사물을 관찰함", "가끔은 한 발 물러서서 내 삶을 관조하는 시간이 필요해.", "아주 좋은 문장입니다. 삶을 대하는 태도로서 '관조'를 잘 활용했습니다.", List.of("그는 세상을 관조하는 태도로 시를 썼다.")));
        realData.add(new DummyItem("낙관", "인생이나 사물을 밝고 희망적으로 봄", "성공을 낙관하기엔 아직 이르다.", "문맥에 딱 맞습니다. '낙관하다'를 부정어와 함께 써서 신중함을 표현했네요.", List.of("지나친 낙관은 금물이다.")));
        realData.add(new DummyItem("도야", "몸과 마음을 닦음", "인격을 도야하기 위해 매일 명상을 합니다.", "좋은 예문입니다. '인격을 도야하다'라는 표현이 자연스럽습니다.", List.of("그는 평생 학문을 도야했다.")));
        realData.add(new DummyItem("불식", "의심이나 오해를 말끔히 없앰", "이번 발표로 세간의 우려를 불식시켰다.", "완벽합니다. 뉴스나 공식 석상에서 자주 쓰이는 고급 표현입니다.", List.of("그는 실력으로 의심을 불식시켰다.")));
        realData.add(new DummyItem("사유", "대상을 두루 생각하는 일", "철학은 깊은 사유를 필요로 한다.", "정확합니다. 단순한 생각이 아닌 깊은 고찰을 '사유'라고 하죠.", List.of("독서는 사유의 힘을 길러준다.")));
        realData.add(new DummyItem("성찰", "자기의 마음을 반성하고 살핌", "성찰 없는 성장은 위험하다.", "교훈적인 의미가 잘 담긴 훌륭한 문장입니다.", List.of("깊은 자기 성찰의 시간을 가졌다.")));
        realData.add(new DummyItem("점철", "어떤 것이 전체에 걸쳐 이어짐", "그의 인생은 고난으로 점철되어 있었다.", "문학적 표현이 돋보입니다. 부정적 상황이 이어질 때 주로 쓰입니다.", List.of("거짓으로 점철된 변명.")));
        realData.add(new DummyItem("타산지석", "남의 잘못을 거울로 삼음", "친구의 실패를 타산지석 삼아 더 노력했다.", "사자성어 활용의 정석입니다. 아주 자연스럽습니다.", List.of("경쟁사의 몰락을 타산지석으로 삼자.")));
        realData.add(new DummyItem("통찰", "본질을 꿰뚫어 봄", "본질을 꿰뚫는 통찰력이 필요하다.", "군더더기 없는 깔끔한 문장입니다.", List.of("그 작가는 인간에 대한 깊은 통찰을 보여준다.")));
        realData.add(new DummyItem("고무적", "힘을 내도록 격려하는", "이번 실험 결과는 매우 고무적이다.", "연구나 비즈니스 상황에서 자주 쓰는 적절한 표현입니다.", List.of("상반기 매출이 고무적이다.")));
        realData.add(new DummyItem("미봉책", "임시변통으로 꿰맞추는 계책", "이건 근본 해결책이 아닌 미봉책일 뿐이야.", "비판적인 시각을 드러낼 때 적확한 단어 선택입니다.", List.of("정부의 대책은 미봉책에 그쳤다.")));
        realData.add(new DummyItem("안목", "사물을 보고 분별하는 견식", "그는 좋은 작품을 고르는 안목이 있다.", "자연스러운 문장입니다.", List.of("사람 보는 안목을 기르자.")));
        realData.add(new DummyItem("여운", "감동이 가시지 않고 남음", "영화가 끝난 후에도 깊은 여운이 남았다.", "감정을 묘사할 때 가장 많이 쓰이는 좋은 표현입니다.", List.of("그의 연설은 긴 여운을 남겼다.")));
        realData.add(new DummyItem("직관", "대상을 직접적으로 파악함", "복잡한 문제일수록 직관을 믿어봐.", "이성적 판단과 대비되는 직관의 의미를 잘 살렸습니다.", List.of("때로는 논리보다 직관이 중요하다.")));
        realData.add(new DummyItem("혜안", "사물을 꿰뚫어 보는 안목", "미래를 내다보는 혜안이 놀랍다.", "상대를 존중하는 뉘앙스가 잘 담겨 있습니다.", List.of("리더에게는 혜안이 필요하다.")));

        // ==========================================
        // [오답 케이스] (유저가 틀리고 AI가 지적함)
        // ==========================================
        realData.add(new DummyItem("와신상담", "복수를 위해 어려움을 견딤",
                "어제 친구랑 와신상담을 나누며 놀았다.",
                "단어 사용이 적절하지 않습니다. '와신상담'은 대화를 나누는 것이 아니라, 복수나 목표를 위해 고통을 참는다는 뜻입니다. '담소'를 쓰시는 게 좋겠습니다.",
                List.of("그는 와신상담하며 재기를 노렸다.", "패배를 딛고 와신상담하여 우승했다.")));

        realData.add(new DummyItem("방관", "직접 관여하지 않고 보기만 함",
                "나는 어제 TV를 방관했다.",
                "어색한 표현입니다. '방관'은 어떤 사건이나 상황을 해결하려 하지 않고 내버려 둔다는 부정적 뉘앙스가 강합니다. TV는 '시청'한다고 해야 합니다.",
                List.of("동료의 어려움을 수수방관해선 안 된다.", "정부는 물가 상승을 방관하고 있다.")));

        realData.add(new DummyItem("격세지감", "오래지 않아 몰라보게 변함",
                "어제 먹은 밥이 격세지감처럼 맛있었다.",
                "틀린 비유입니다. '격세지감'은 세월이 흘러 세상이 크게 변한 느낌을 말합니다. 맛이나 감동을 표현할 때 쓰는 말이 아닙니다.",
                List.of("10년 만에 고향에 오니 격세지감이 든다.", "기술의 발전 속도에 격세지감을 느낀다.")));

        realData.add(new DummyItem("반추", "지난 일을 되풀이하여 생각함",
                "소는 위가 4개라서 음식을 반추한다.",
                "과학적으로는 맞는 말이지만, 어휘 학습 문맥에서는 아쉽습니다. 문학적으로 '지나간 일을 되돌아본다'는 의미로 문장을 만들어보세요.",
                List.of("지난 실수를 반추하며 반성했다.", "추억을 반추하니 미소가 지어졌다.")));

        realData.add(new DummyItem("기연", "기이한 인연",
                "우리는 기연이라서 매일 싸운다.",
                "문맥이 조금 어색합니다. '기연'은 보통 신기하고 소중한 인연을 뜻할 때 긍정적으로 쓰입니다. 싸우는 관계라면 '악연'이 더 어울릴 수 있습니다.",
                List.of("그곳에서 너를 만난 건 기연이었다.", "소설 같은 기연으로 부부가 되었다.")));

        realData.add(new DummyItem("위선", "겉으로만 착한 체함",
                "그는 위선을 떨어서 착한 상을 받았다.",
                "인과관계가 어색합니다. 위선은 겉으로만 착한 척하는 것이므로, 결국에는 비판받는 문맥에 주로 쓰입니다.",
                List.of("가식과 위선을 벗어던져라.", "그것은 위선적인 행동일 뿐이다.")));

        realData.add(new DummyItem("배척", "거부하여 밀어 내침",
                "나는 배가 고파서 밥을 배척했다.",
                "완전히 잘못된 사용입니다. '배척'은 싫어서 밀어낸다는 뜻입니다. 배가 고픈데 밥을 거부하는 건 문맥상 맞지 않습니다.",
                List.of("이질적인 문화를 배척하지 마라.", "그는 조직 내에서 배척당했다.")));

        realData.add(new DummyItem("유대", "밀접하게 연결된 관계",
                "우리 반은 유대감이 없어서 너무 친하다.",
                "모순된 문장입니다. 유대감이 없으면 친하지 않다는 뜻이고, 유대감이 깊어야 친한 것입니다.",
                List.of("가족 간의 유대를 강화해야 한다.", "우리는 끈끈한 유대감을 공유하고 있다.")));


        // 데이터 개수 체크
        if (days * 3 > realData.size()) {
            return "오류: 더미 데이터가 부족합니다. (요청: " + (days * 3) + "개, 보유: " + realData.size() + "개)";
        }

        // 데이터 섞기
        Collections.shuffle(realData);
        int cursor = 0;

        // 7일치 루프 (7 -> 6 -> ... -> 1일 전)
        for (int i = days; i >= 1; i--) {
            LocalDate pastDate = todayKst.minusDays(i);

            List<String> dailyWordIds = new ArrayList<>();

            for (int j = 0; j < 3; j++) {
                DummyItem item = realData.get(cursor++);

                Word word = wordRepository.findByWord(item.word())
                        .orElseGet(() -> wordRepository.save(Word.builder()
                                .word(item.word())
                                .meaning(item.meaning())
                                .build()));

                dailyWordIds.add(word.getId());

                LearningHistory history = LearningHistory.builder()
                        .userId(userId)
                        .wordId(word.getId())
                        .userSentence(item.userSentence())
                        .aiEvaluation(item.aiEvaluation())
                        .aiSentences(item.aiExamples())
                        // ★ KST 기준 과거 날짜 생성
                        .createdAt(pastDate.atTime(12, 0, 0))
                        .build();

                learningHistoryRepository.save(history);
            }

            // DailyLog 생성
            dailyLogRepository.deleteByUserIdAndDate(userId, pastDate);

            DailyLog log = DailyLog.builder()
                    .userId(userId)
                    .date(pastDate)
                    .wordIds(dailyWordIds)
                    .completedWordIds(new ArrayList<>(dailyWordIds))
                    .build();

            dailyLogRepository.save(log);
        }

        // 스트릭 업데이트
        UserProgress progress = userProgressRepository.findById(userId)
                .orElse(UserProgress.builder().userId(userId).build());

        progress.setCurrentStreak(days);
        progress.setTodayCompleted(false);
        progress.setLastLearningDate(todayKst.minusDays(1)); // 어제

        userProgressRepository.save(progress);

        return String.format("%d일치 리얼 데이터(오답 포함) 생성 완료! (스트릭: %d)", days, days);
    }
}