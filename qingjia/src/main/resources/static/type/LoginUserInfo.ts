import { Teacher } from './Teacher';
import { Student } from './Student';
export interface LoginUserInfo {
    type: number;
    student:Student;
    teacher:Teacher;
    roleType: number;
}
