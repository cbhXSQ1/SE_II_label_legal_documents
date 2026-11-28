package com.seii.legal.data.repository;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.ClassMember;
import com.seii.legal.data.entity.ImportRecord;
import com.seii.legal.data.entity.StudentAccountResult;
import com.seii.legal.data.entity.TeachingClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeachingAdminRepository {
    String saveClass(TeachingClass teachingClass);

    Optional<TeachingClass> findClass(String classId);

    Optional<TeachingClass> findClassByCode(String classCode);

    /** 附录协同项：学生端定位本人所属班级。 */
    Optional<TeachingClass> findClassByStudentId(String studentId);

    PageResult<TeachingClass> findClasses(String term, String teacherId, int page, int size);

    void saveClassMember(String classId, String studentId, String studentNo, LocalDateTime joinedAt, String status);

    List<ClassMember> findClassMembers(String classId);

    void transferClass(String studentId, String fromClassId, String toClassId, LocalDateTime time);

    String saveImportRecord(ImportRecord record);

    Optional<ImportRecord> findImportRecord(String importId);

    Optional<Account> findByStudentNo(String studentNo);

    Optional<Account> findByTeacherNo(String teacherNo);

    StudentAccountResult createStudentAccount(String studentNo, String name, String classId);
}
