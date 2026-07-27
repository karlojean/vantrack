package com.vantrack.vans;

import com.vantrack.users.User;
import com.vantrack.vans.web.dto.VanFilter;
import com.vantrack.vans.web.dto.VanResponse;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListAllVansUseCase {

    private final VanRepository vanRepository;

    public ListAllVansUseCase(VanRepository vanRepository) {
        this.vanRepository = vanRepository;
    }

    public List<VanResponse> execute(VanFilter filter) {
        Specification<Van> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if(filter.plate() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("plate")), "%" + filter.plate().toLowerCase() + "%"));
        }

        if(filter.userId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("driver").get("id"), filter.userId())
            );
        }

        List<Van> vans = vanRepository.findAll(spec);


        return vans.stream().map(VanResponse::fromEntity).toList();
    }

}
