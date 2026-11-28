package com.seii.legal.rule.teaching;

import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.ClassMember;
import com.seii.legal.data.entity.ImportRow;
import com.seii.legal.data.entity.TeachingClass;

import java.util.List;
import java.util.Optional;

public interface TeachingAdminRule {
    String classAction(String classCode, List<TeachingClass> existing);

    boolean uniqueStudentNo(String studentNo, List<Account> existing);

    String studentClass(String studentId, String classId, List<ClassMember> members);

    Optional<String> matchTeacher(String teacherNo, List<Account> teachers);

    ImportRow checkRow(ImportRow row);

    Account studentAccount(String studentNo, String name);
}
