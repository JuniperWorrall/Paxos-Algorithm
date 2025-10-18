package com.example;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest 
{
    private static List<CouncilMember> Members;
    private static int NUM_MEMBERS = 9;

    private void LaunchMembers(Profile... Profiles) throws Exception{
        Members = new ArrayList<>();
        for (int i = 0; i < NUM_MEMBERS; i++) {
            String MemberId = "M" + (i + 1);
            Profile MemberProfile = (Profiles.length > i) ? Profiles[i] : Profile.RELIABLE;
            CouncilMember Member = new CouncilMember(MemberId, MemberProfile, "network_config");
            Members.add(Member);
        }
        Thread.sleep(500);
    }

    private String WaitForConsensus(long TimeoutMillis) throws InterruptedException {
        long StartTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - StartTime < TimeoutMillis) {
            for (CouncilMember Member : Members) {
                String Result = Member.GetLearner().GetConsensusValue();
                if (Result != null) {
                    return Result;
                }
            }
            Thread.sleep(200);
        }
        return null; 
    }

    @AfterEach
    void TearDown() {
        for (CouncilMember M : Members) {
            M.Shutdown();
        }
        Members.clear();

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    @Test
    @Order(1)
    public void TestIdealNetwork() throws Exception {
        LaunchMembers();
        Members.get(3).GetProposer().InitiateProposal("M5");
        String Result = WaitForConsensus(5000);

        assertNotNull(Result, "Consensus should be reached");
        assertEquals("M5", Result, "M5 should be elected");
    }

    @Test
    @Order(2)
    public void TestConcurrentProposals() throws Exception {
        LaunchMembers();
        Members.get(0).GetProposer().InitiateProposal("M1");
        Members.get(7).GetProposer().InitiateProposal("M8");

        String Result = WaitForConsensus(8000);

        assertNotNull(Result, "Consensus should be reached");
        assertTrue(Result.equals("M1") || Result.equals("M8"), "Consensus should pick one valid candidate");
    }

    @Test
    @Order(3)
    public void TestFaultTolerance() throws Exception{
        LaunchMembers(
            Profile.RELIABLE,
            Profile.LATENT,
            Profile.FAILURE,
            Profile.STANDARD, Profile.STANDARD,
            Profile.STANDARD, Profile.STANDARD,
            Profile.STANDARD, Profile.STANDARD
        );

        Members.get(3).GetProposer().InitiateProposal("M4");
        String Result1 = WaitForConsensus(8000);
        assertNotNull(Result1, "Consensus should succeed despite failures");
        
        Members.get(1).GetProposer().InitiateProposal("M2");
        String Result2 = WaitForConsensus(8000);
        assertNotNull(Result2, "Consensus should succeed with latency");

        CouncilMember FailedMember = Members.get(2);
        FailedMember.GetProposer().InitiateProposal("M3");
        FailedMember.Shutdown();
        String Result3 = WaitForConsensus(8000);
        assertNotNull(Result3, "Consensus recover from member failure and reach consensus");
    }
}
